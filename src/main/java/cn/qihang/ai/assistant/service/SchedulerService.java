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

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Shanghai")
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
                // 循环任务由周期调度驱动，不参与到期触发（截止日期仅一次性/无调度任务使用）
                if (TaskService.isCycleTask(t)) {
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
            String today = cn.qihang.ai.assistant.util.TimeUtil.todayStr();
            List<TaskItem> tasks = taskService.getAllTasks();
            for (TaskItem t : tasks) {
                if (t == null || "done".equals(t.status)) {
                    continue;
                }
                // 循环任务：周期已结束 → 自动转为已完成
                if (TaskService.isCycleTask(t) && TaskService.isCycleEnded(t)) {
                    log.info("[{}] ⏰ 循环任务周期结束，转为已完成: {}", TimeUtil.nowStr(), t.title);
                    taskService.markTaskDone(t.id);
                    continue;
                }
                // 已有排队/执行中的记录则跳过，避免重复入队
                if (taskService.hasActiveExecution(t.id)) {
                    continue;
                }

                boolean isOnce = "once".equals(t.scheduleType)
                        || (t.scheduleType == null && t.scheduledStart != null && !t.scheduledStart.isBlank());
                if (isOnce) {
                    checkOnceTask(t, now, today);
                } else if (TaskService.isCycleTask(t)) {
                    checkCycleTask(t, now, today);
                }
            }
        } catch (Exception e) {
            log.error("[任务] 检查定时启动失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 一次性定时任务：到点触发一次，当天去重。
     */
    private void checkOnceTask(TaskItem t, String now, String today) {
        if (t.scheduledStart == null || t.scheduledStart.isBlank()) {
            return;
        }
        if (t.scheduledStart.compareTo(now) > 0) {
            return;
        }
        // 当天已触发过则跳过，避免每分钟重复触发
        if (today.equals(t.lastReminded)) {
            return;
        }
        triggerTask(t, today, "scheduled");
    }

    /**
     * 循环任务：按周期（daily/weekly/monthly/cron）判断当前是否到点。
     */
    private void checkCycleTask(TaskItem t, String now, String today) {
        if (!isCycleDue(t, now, today)) {
            return;
        }
        triggerTask(t, today, "scheduled");
    }

    private void triggerTask(TaskItem t, String today, String triggerType) {
        if ("ai".equals(t.action)) {
            log.info("[{}] ⏰ 触发 AI 任务：{}", TimeUtil.nowStr(), t.title);
            enqueueTask(t, triggerType);
            if (!TaskService.isCycleTask(t)) {
                // 一次性任务按天去重；循环任务由入队时记录 last_cycle_run
                taskService.markTaskReminded(t.id, today);
            }
            logService.add("任务中心", "成功", "触发 AI 任务: " + t.title);
        } else {
            log.info("[{}] ⏰ 触发任务提醒：{}", TimeUtil.nowStr(), t.title);
            pushTaskDue(t, false);
            if (TaskService.isCycleTask(t)) {
                taskService.markTaskCycleRun(t.id, null);
            } else {
                taskService.markTaskReminded(t.id, today);
            }
            logService.add("任务中心", "成功", "任务提醒: " + t.title);
        }
    }

    /**
     * 判断循环任务当前是否到点触发。
     * daily/weekly/monthly 按 cycle_time 判定当天执行时间；cron 判定最近一次触发是否落在一分钟内。
     */
    private boolean isCycleDue(TaskItem t, String now, String today) {
        String cycleType = t.cycleType;
        if ("cron".equals(cycleType)) {
            return isCronDue(t.cycleValue, t.lastCycleRun);
        }
        // daily/weekly/monthly 需要执行时间
        if (t.cycleTime == null || t.cycleTime.isBlank()) {
            return false;
        }
        // now 格式为 yyyy-MM-dd HH:mm:ss，第 11-16 位是 HH:mm
        if (now.length() < 16 || now.substring(11, 16).compareTo(t.cycleTime.trim()) < 0) {
            return false;
        }
        // 当天已执行过则跳过
        if (t.lastCycleRun != null && t.lastCycleRun.length() >= 10
                && today.equals(t.lastCycleRun.substring(0, 10))) {
            return false;
        }
        LocalDate date = LocalDate.parse(today);
        if ("daily".equals(cycleType)) {
            return true;
        }
        if ("weekly".equals(cycleType)) {
            // cycle_value: 1-7 逗号分隔（1=周一 ... 7=周日）
            int dayOfWeek = date.getDayOfWeek().getValue();
            return isInCycleValue(t.cycleValue, dayOfWeek);
        }
        if ("monthly".equals(cycleType)) {
            return isInCycleValue(t.cycleValue, date.getDayOfMonth());
        }
        return false;
    }

    private boolean isInCycleValue(String cycleValue, int value) {
        if (cycleValue == null || cycleValue.isBlank()) {
            return false;
        }
        for (String part : cycleValue.split(",")) {
            try {
                if (Integer.parseInt(part.trim()) == value) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * cron 表达式：最近一次触发时间是否落在上一分钟内，且未执行过。
     */
    private boolean isCronDue(String cron, String lastCycleRun) {
        if (cron == null || cron.isBlank()) {
            return false;
        }
        try {
            org.springframework.scheduling.support.CronExpression expr =
                    org.springframework.scheduling.support.CronExpression.parse(cron);
            java.time.Instant now = java.time.Instant.now();
            java.time.Instant windowStart = now.minusSeconds(60);
            java.time.Instant next = expr.next(windowStart);
            if (next == null || next.isAfter(now)) {
                return false;
            }
            String fireStr = next.atZone(java.time.ZoneId.of("Asia/Shanghai"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return lastCycleRun == null || lastCycleRun.isBlank() || lastCycleRun.compareTo(fireStr) < 0;
        } catch (Exception e) {
            log.warn("[任务] cron 表达式解析失败: {} - {}", cron, e.getMessage());
            return false;
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