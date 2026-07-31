package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.model.TaskData.TaskItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 任务 AI 执行器 — 队列式执行。
 * 任务触发后先入队排队，由工作线程按 FIFO 顺序执行，执行过程和结果写入 task_executions 记录。
 */
@Service
public class TaskAgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskAgentExecutor.class);
    private static final int QUEUE_CAPACITY = 100;
    private static final int WORKER_COUNT = 3;

    private final NoteAssistantService noteAssistantService;
    private final FeishuService feishuService;
    private final LogService logService;
    private final TaskService taskService;

    private final BlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final List<Thread> workers = new ArrayList<>();

    public TaskAgentExecutor(NoteAssistantService noteAssistantService,
                             FeishuService feishuService,
                             LogService logService,
                             TaskService taskService) {
        this.noteAssistantService = noteAssistantService;
        this.feishuService = feishuService;
        this.logService = logService;
        this.taskService = taskService;
        for (int i = 0; i < WORKER_COUNT; i++) {
            Thread worker = new Thread(this::workLoop, "task-agent-worker-" + i);
            worker.setDaemon(true);
            workers.add(worker);
            worker.start();
        }
        log.info("TaskAgentExecutor started with {} workers, queue capacity {}", WORKER_COUNT, QUEUE_CAPACITY);
    }

    private static class QueueEntry {
        final TaskItem task;
        final Long kbId;
        final String triggerType;
        final String triggeredBy;
        final String executionId;

        QueueEntry(TaskItem task, Long kbId, String triggerType, String triggeredBy) {
            this.task = task;
            this.kbId = kbId;
            this.triggerType = triggerType;
            this.triggeredBy = triggeredBy;
            this.executionId = "EX" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        }
    }

    private void workLoop() {
        while (true) {
            try {
                QueueEntry entry = queue.take();
                runEntry(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void runEntry(QueueEntry entry) {
        TaskItem task = entry.task;
        String executionId = entry.executionId;
        try {
            taskService.appendExecutionLog(executionId, "[" + cn.qihang.ai.assistant.util.TimeUtil.nowStr() + "] 开始执行任务: " + task.title);
            taskService.startExecution(executionId);

            String result = execute(task, entry.kbId);

            taskService.appendExecutionLog(executionId, "[" + cn.qihang.ai.assistant.util.TimeUtil.nowStr() + "] AI 执行完成，准备推送结果");
            taskService.completeExecution(executionId, result != null ? result : "（AI 未返回内容）");

            try {
                String title = "🤖 AI 任务完成: " + task.title;
                List<List<Map<String, String>>> paragraphs = feishuService.reportToParagraphs(result != null ? result : "（AI 未返回内容）");
                feishuService.sendPost(title, paragraphs);
                taskService.appendExecutionLog(executionId, "[" + cn.qihang.ai.assistant.util.TimeUtil.nowStr() + "] 结果已推送到飞书");
            } catch (Exception e) {
                log.warn("[任务Agent] 推送失败: {}", e.getMessage());
                taskService.appendExecutionLog(executionId, "[" + cn.qihang.ai.assistant.util.TimeUtil.nowStr() + "] 推送飞书失败: " + e.getMessage());
            }

            logService.add("任务中心", "成功", "AI 任务执行完成: " + task.title);
            log.info("[任务Agent] 执行完成并推送: {}", task.title);
        } catch (Exception e) {
            log.error("[任务Agent] 执行失败: {} - {}", task.title, e.getMessage());
            taskService.appendExecutionLog(executionId, "[" + cn.qihang.ai.assistant.util.TimeUtil.nowStr() + "] 执行失败: " + e.getMessage());
            taskService.failExecution(executionId, e.getMessage());
            logService.add("任务中心", "失败", "AI 任务执行失败: " + task.title + " - " + e.getMessage());
            fallbackPush(task, e.getMessage());
        }
    }

    /**
     * 任务入队排队执行（手动触发 / 定时触发均可走此入口）。
     * 返回排队信息（executionId、队列位置）；队列已满返回 null。
     */
    public Map<String, Object> enqueue(TaskItem task, Long kbId, String triggerType, String triggeredBy) {
        QueueEntry entry = new QueueEntry(task, kbId, triggerType, triggeredBy);
        boolean offered = queue.offer(entry);
        if (!offered) {
            throw new IllegalStateException("任务队列已满（最多 " + QUEUE_CAPACITY + " 个排队），请稍后再试");
        }
        taskService.createExecution(entry.executionId, task.id, task.title, triggerType, triggeredBy);
        int position = getQueuePosition(task.id);
        log.info("[任务Agent] 任务入队: {} (execution={}, position={}, trigger={})",
                task.title, entry.executionId, position, triggerType);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("executionId", entry.executionId);
        info.put("position", position);
        return info;
    }

    /**
     * 兼容旧调用：异步执行（走同一队列）。
     */
    public void executeAsync(TaskItem task, Long kbId) {
        try {
            enqueue(task, kbId, "manual", "user");
        } catch (IllegalStateException e) {
            log.warn("[任务Agent] 入队失败: {}", e.getMessage());
        }
    }

    /**
     * 取消排队中的任务（已在执行中无法取消）。
     */
    public boolean cancelQueued(Long taskId) {
        boolean removed = queue.removeIf(e -> taskId.equals(e.task.id));
        if (removed) {
            taskService.getTaskExecutions(taskId).stream()
                    .filter(e -> "QUEUED".equals(e.status))
                    .findFirst()
                    .ifPresent(e -> taskService.cancelExecution(e.executionId));
            log.info("[任务Agent] 已取消排队任务: {}", taskId);
        }
        return removed;
    }

    /**
     * 队列信息：任务ID -> 排队位置（1 起）。正在执行中的任务不在此列表。
     */
    public Map<Long, Integer> getQueueInfo() {
        Map<Long, Integer> info = new LinkedHashMap<>();
        int pos = 1;
        for (QueueEntry entry : queue) {
            info.put(entry.task.id, pos++);
        }
        return info;
    }

    private int getQueuePosition(Long taskId) {
        int pos = 1;
        for (QueueEntry entry : queue) {
            if (taskId.equals(entry.task.id)) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    public int getQueueSize() {
        return queue.size();
    }

    /**
     * 同步执行 AI 任务，返回 AI 结果文本。
     */
    public String execute(TaskItem task, Long kbId) throws Exception {
        String sessionId = "task-" + task.id;
        String prompt = (task.actionPrompt != null && !task.actionPrompt.isBlank())
                ? task.actionPrompt
                : "请执行任务「" + task.title + "」" + (task.description != null ? "，任务描述: " + task.description : "")
                  + "，完成后总结执行结果";
        log.info("[任务Agent] 开始执行: {} (session={}, kbId={})", task.title, sessionId, kbId);
        return noteAssistantService.chat(sessionId, prompt, "knowledge", kbId);
    }

    private void fallbackPush(TaskItem task, String error) {
        try {
            List<List<Map<String, String>>> content = List.of(
                    List.of(Map.of("tag", "text", "text", "⚠️ AI 任务「" + task.title + "」执行失败")),
                    List.of(Map.of("tag", "text", "text", error != null ? error : "未知错误"))
            );
            feishuService.sendPost("⚠️ AI 任务失败: " + task.title, content);
        } catch (Exception e) {
            log.warn("[任务Agent] 失败回退推送异常: {}", e.getMessage());
        }
    }
}
