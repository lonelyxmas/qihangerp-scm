package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.model.TaskData.TaskItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务 AI 执行器 — 到期 AI 任务自动调用 Agent 执行，结果推送到飞书。
 * 复用 NoteAssistantService 的完整工具链（知识库检索 + 数据集 + 互联网搜索）。
 */
@Service
public class TaskAgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskAgentExecutor.class);

    private final NoteAssistantService noteAssistantService;
    private final FeishuService feishuService;
    private final LogService logService;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "task-agent-executor");
        t.setDaemon(true);
        return t;
    });

    public TaskAgentExecutor(NoteAssistantService noteAssistantService,
                             FeishuService feishuService,
                             LogService logService) {
        this.noteAssistantService = noteAssistantService;
        this.feishuService = feishuService;
        this.logService = logService;
    }

    /**
     * 异步执行 AI 任务并推送结果到飞书，不阻塞调度轮询。
     */
    public void executeAsync(TaskItem task, Long kbId) {
        executor.submit(() -> {
            try {
                String result = execute(task, kbId);
                String title = "🤖 AI 任务完成: " + task.title;
                List<List<Map<String, String>>> paragraphs = feishuService.reportToParagraphs(result != null ? result : "（AI 未返回内容）");
                feishuService.sendPost(title, paragraphs);
                logService.add("任务中心", "成功", "AI 任务执行完成: " + task.title);
                log.info("[任务Agent] 执行完成并推送: {}", task.title);
            } catch (Exception e) {
                log.error("[任务Agent] 执行失败: {} - {}", task.title, e.getMessage());
                logService.add("任务中心", "失败", "AI 任务执行失败: " + task.title + " - " + e.getMessage());
                fallbackPush(task, e.getMessage());
            }
        });
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
