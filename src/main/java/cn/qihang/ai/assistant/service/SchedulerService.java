package cn.qihang.ai.assistant.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.model.ReminderData.Reminder;
import cn.qihang.ai.assistant.model.TaskData.TaskItem;
import cn.qihang.ai.assistant.util.TimeUtil;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final ReportService reportService;
    private final LogService logService;
    private final ReminderService reminderService;
    private final KbBaseService kbService;
    private final TaskService taskService;
    private final TaskAgentExecutor taskAgentExecutor;
    private final FeishuService feishuService;

    public SchedulerService(ReportService reportService,
                            LogService logService,
                            ReminderService reminderService,
                            KbBaseService kbService,
                            TaskService taskService,
                            TaskAgentExecutor taskAgentExecutor,
                            FeishuService feishuService) {
        this.reportService = reportService;
        this.logService = logService;
        this.reminderService = reminderService;
        this.kbService = kbService;
        this.taskService = taskService;
        this.taskAgentExecutor = taskAgentExecutor;
        this.feishuService = feishuService;
    }

    @Scheduled(cron = "0 0 11 * * ?", zone = "Asia/Shanghai")
    public void morningReport() {
        log.info("[{}] ⏰ 定时任务：生成综合日报", TimeUtil.nowStr());
        List<KbBaseEntity> kbs = kbService.getAll();
        for (KbBaseEntity kb : kbs) {
            try {
                boolean autoReport = kb.getAutoReport() != null && kb.getAutoReport() == 1;
                if (!autoReport) {
                    log.info("[{}] ⏰ 知识库「{}」已关闭自动日报，跳过", TimeUtil.nowStr(), kb.getName());
                    continue;
                }
                log.info("[{}] ⏰ 为知识库「{}」生成日报", TimeUtil.nowStr(), kb.getName());
                reportService.generateAndPush(kb.getId());
            } catch (Exception e) {
                log.error("[{}] ⏰ 知识库「{}」日报生成失败: {}", TimeUtil.nowStr(), kb.getName(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void checkDynamicReminders() {
        try {
            List<Reminder> dueReminders = reminderService.getDueReminders();
            if (!dueReminders.isEmpty()) {
                for (Reminder r : dueReminders) {
                    String reminderName = r.name != null ? r.name : "(未命名提醒)";
                    log.info("[{}] ⏰ 触发动态提醒：{}", TimeUtil.nowStr(), reminderName);
                    reminderService.triggerReminder(r);
                    logService.add("定时提醒", "成功", reminderName);
                }
            }
        } catch (Exception e) {
            log.error("[提醒] 检查动态提醒失败: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void checkDueTasks() {
        try {
            String today = LocalDate.now().toString();
            String now = cn.qihang.ai.assistant.util.TimeUtil.nowStr();
            List<TaskItem> tasks = taskService.getAllTasks();
            for (TaskItem t : tasks) {
                if (t == null || "done".equals(t.status) || t.dueDate == null || t.dueDate.isBlank()) {
                    continue;
                }
                boolean isDueToday = t.dueDate.equals(today);
                boolean isOverdue = t.dueDate.compareTo(today) < 0;
                if (!isDueToday && !isOverdue) {
                    continue;
                }
                // 按天去重：当天已提醒过不再提醒
                if (today.equals(t.lastReminded)) {
                    continue;
                }

                if ("ai".equals(t.action)) {
                    log.info("[{}] ⏰ 触发 AI 任务：{}", TimeUtil.nowStr(), t.title);
                    enqueueTask(t, "due");
                    taskService.markTaskReminded(t.id, today);
                    logService.add("任务中心", "成功", "触发 AI 任务: " + t.title);
                } else {
                    log.info("[{}] ⏰ 触发任务到期提醒：{} (今天到期={}, 逾期={})",
                            TimeUtil.nowStr(), t.title, isDueToday, isOverdue);
                    pushTaskDue(t, isDueToday);
                    taskService.markTaskReminded(t.id, today);
                    logService.add("任务中心", "成功", "到期提醒: " + t.title);
                }
            }
        } catch (Exception e) {
            log.error("[任务] 检查任务到期失败: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void checkScheduledStart() {
        try {
            String now = cn.qihang.ai.assistant.util.TimeUtil.nowStr();
            List<TaskItem> tasks = taskService.getAllTasks();
            for (TaskItem t : tasks) {
                if (t == null || "done".equals(t.status)) {
                    continue;
                }
                if (t.scheduledStart == null || t.scheduledStart.isBlank()) {
                    continue;
                }
                if (t.scheduledStart.compareTo(now) > 0) {
                    continue;
                }
                // 已有排队/执行中的记录则跳过，避免重复入队
                if (taskService.hasActiveExecution(t.id)) {
                    continue;
                }
                if ("ai".equals(t.action)) {
                    log.info("[{}] ⏰ 定时启动 AI 任务：{} (计划时间: {})", TimeUtil.nowStr(), t.title, t.scheduledStart);
                    enqueueTask(t, "scheduled");
                    logService.add("任务中心", "成功", "定时启动 AI 任务: " + t.title);
                } else {
                    log.info("[{}] ⏰ 定时启动普通任务：{} (计划时间: {})", TimeUtil.nowStr(), t.title, t.scheduledStart);
                    pushTaskDue(t, false);
                    taskService.markTaskReminded(t.id, cn.qihang.ai.assistant.util.TimeUtil.todayStr());
                    logService.add("任务中心", "成功", "定时启动任务: " + t.title);
                }
            }
        } catch (Exception e) {
            log.error("[任务] 检查定时启动失败: {}", e.getMessage(), e);
        }
    }

    private void enqueueTask(TaskItem t, String triggerType) {
        try {
            taskAgentExecutor.enqueue(t, t.kbId, triggerType, "system");
        } catch (Exception e) {
            log.warn("[任务] 任务入队失败: {} - {}", t.title, e.getMessage());
        }
    }

    private void pushTaskDue(TaskItem t, boolean isDueToday) {
        try {
            String title = isDueToday ? "📌 今日到期任务" : "⚠️ 任务已逾期";
            StringBuilder sb = new StringBuilder();
            sb.append(isDueToday ? "今天有一个任务到期，记得处理：\n" : "以下任务已逾期，请尽快处理：\n");
            sb.append("\n· ").append(t.title);
            if (t.description != null && !t.description.isBlank()) {
                sb.append("\n  描述: ").append(t.description.length() > 100
                        ? t.description.substring(0, 100) + "..." : t.description);
            }
            sb.append("\n· 截止日期: ").append(t.dueDate);
            if ("ai".equals(t.action)) {
                sb.append("\n· 类型: 🤖 AI 任务（到期自动执行）");
            }
            feishuPush(title, sb.toString());
        } catch (Exception e) {
            log.error("[任务] 到期提醒推送失败: {} - {}", t.title, e.getMessage());
        }
    }

    private void feishuPush(String title, String message) {
        try {
            var content = List.of(
                    List.of(Map.of("tag", "text", "text", "━━━━━━━━━━━━━━━━━━")),
                    List.of(Map.of("tag", "text", "text", message)),
                    List.of(Map.of("tag", "text", "text", "━━━━━━━━━━━━━━━━━━")),
                    List.of(Map.of("tag", "text", "text", "去任务中心处理: /tasks"))
            );
            feishuService.sendPost(title, content);
        } catch (Exception e) {
            log.warn("[任务] 推送失败: {}", e.getMessage());
        }
    }
}