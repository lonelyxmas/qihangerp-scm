package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.model.TaskData.*;
import cn.qihang.ai.assistant.model.ReminderData.Reminder;
import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.TaskService;
import cn.qihang.ai.assistant.service.ReminderService;
import cn.qihang.ai.assistant.service.TaskAgentExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class PlannerApiController {

    private final TaskService taskService;
    private final ReminderService reminderService;
    private final LogService logService;
    private final TaskAgentExecutor taskAgentExecutor;

    public PlannerApiController(TaskService taskService, ReminderService reminderService,
                                LogService logService, TaskAgentExecutor taskAgentExecutor) {
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.logService = logService;
        this.taskAgentExecutor = taskAgentExecutor;
    }

    @GetMapping("/api/tasks")
    @ResponseBody
    public Map<String, Object> getTasks(@RequestParam(required = false) Long kbId) {
        List<TaskItem> tasks = taskService.getAllTasks();
        return Map.of("ok", true, "tasks", tasks);
    }

    @PostMapping("/api/tasks/add")
    @ResponseBody
    public Map<String, Object> addTask(
            @RequestParam(required = false) Long kbId,
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam(required = false, defaultValue = "mid") String priority,
            @RequestParam(required = false, defaultValue = "") String dueDate,
            @RequestParam(required = false, defaultValue = "") String action,
            @RequestParam(required = false, defaultValue = "") String actionPrompt) {
        try {
            TaskItem task = taskService.addTask(title, description, priority, dueDate, kbId, action, actionPrompt);
            logService.add("任务中心", "成功", "添加任务: " + title);
            return Map.of("ok", true, "task", task);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/tasks/update")
    @ResponseBody
    public Map<String, Object> updateTask(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actionPrompt) {
        try {
            TaskItem task = taskService.updateTask(id, title, description, status, priority, dueDate, kbId, action, actionPrompt);
            if (task == null) {
                return Map.of("ok", false, "error", "任务不存在");
            }
            logService.add("任务中心", "成功", "更新任务: " + task.title);
            return Map.of("ok", true, "task", task);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/tasks/delete")
    @ResponseBody
    public Map<String, Object> deleteTask(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id) {
        try {
            boolean ok = taskService.deleteTask(id, kbId);
            if (ok) {
                logService.add("任务中心", "成功", "删除任务: " + id);
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/tasks/execute")
    @ResponseBody
    public Map<String, Object> executeTask(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id) {
        try {
            TaskItem task = taskService.getAllTasks().stream()
                    .filter(x -> x.id.equals(id))
                    .findFirst().orElse(null);
            if (task == null) {
                return Map.of("ok", false, "error", "任务不存在");
            }
            taskAgentExecutor.executeAsync(task, kbId != null ? kbId : task.kbId);
            logService.add("任务中心", "成功", "手动触发 AI 执行: " + task.title);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/api/reminders")
    @ResponseBody
    public List<Map<String, Object>> getReminders(@RequestParam(required = false) Long kbId) {
        return reminderService.getAllReminders().stream()
                .map(r -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", r.id);
                    m.put("name", r.name);
                    m.put("message", r.message);
                    m.put("type", r.type);
                    m.put("time", r.time);
                    m.put("date", r.date);
                    m.put("dayOfWeek", r.dayOfWeek);
                    m.put("dayOfMonth", r.dayOfMonth);
                    m.put("monthDay", r.monthDay);
                    m.put("enabled", r.enabled);
                    m.put("createdAt", r.createdAt);
                    m.put("lastTriggered", r.lastTriggered);
                    m.put("description", reminderService.getReminderDescription(r));
                    return m;
                })
                .toList();
    }

    @PostMapping("/api/reminders/add")
    @ResponseBody
    public Map<String, Object> addReminder(
            @RequestParam(required = false) Long kbId,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String message,
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "09:00") String time,
            @RequestParam(required = false, defaultValue = "") String date,
            @RequestParam(required = false, defaultValue = "") String dayOfWeek,
            @RequestParam(required = false, defaultValue = "") String dayOfMonth,
            @RequestParam(required = false, defaultValue = "") String monthDay) {
        try {
            Reminder r = reminderService.addReminder(name, message, type, time,
                    date, dayOfWeek, dayOfMonth, monthDay, kbId);
            logService.add("提醒管理", "成功", "添加提醒: " + name);
            return Map.of("ok", true, "reminder", r);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/reminders/update")
    @ResponseBody
    public Map<String, Object> updateReminder(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String dayOfWeek,
            @RequestParam(required = false) String dayOfMonth,
            @RequestParam(required = false) String monthDay,
            @RequestParam(required = false) Boolean enabled) {
        try {
            boolean ok = reminderService.updateReminder(id, name, message, type, time,
                    date, dayOfWeek, dayOfMonth, monthDay, enabled, kbId);
            if (!ok) {
                return Map.of("ok", false, "error", "提醒不存在");
            }
            logService.add("提醒管理", "成功", "更新提醒: " + name);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/reminders/delete")
    @ResponseBody
    public Map<String, Object> deleteReminder(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id) {
        try {
            boolean ok = reminderService.deleteReminder(id, kbId);
            if (ok) {
                logService.add("提醒管理", "成功", "删除提醒");
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/reminders/toggle")
    @ResponseBody
    public Map<String, Object> toggleReminder(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id) {
        try {
            boolean ok = reminderService.toggleReminder(id, kbId);
            if (ok) {
                logService.add("提醒管理", ok ? "启用" : "禁用", "切换提醒状态");
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/reminders/trigger")
    @ResponseBody
    public Map<String, Object> triggerReminder(
            @RequestParam(required = false) Long kbId,
            @RequestParam String id) {
        try {
            List<Reminder> reminders = reminderService.getAllReminders();
            Reminder r = reminders.stream().filter(x -> x.id.equals(id)).findFirst().orElse(null);
            if (r == null) {
                return Map.of("ok", false, "error", "提醒不存在");
            }
            reminderService.triggerReminder(r, kbId);
            logService.add("提醒管理", "成功", "手动触发: " + r.name);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}