package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.model.TaskData.*;
import cn.qihang.ai.assistant.model.ReminderData.Reminder;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.TokenService;
import cn.qihang.ai.assistant.service.LogService;
import cn.qihang.ai.assistant.service.TaskService;
import cn.qihang.ai.assistant.service.ReminderService;
import cn.qihang.ai.assistant.service.TaskAgentExecutor;
import cn.qihang.ai.assistant.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PlannerApiController {

    private final TaskService taskService;
    private final ReminderService reminderService;
    private final LogService logService;
    private final TaskAgentExecutor taskAgentExecutor;
    private final TokenService tokenService;
    private final ISysUserService sysUserService;

    public PlannerApiController(TaskService taskService, ReminderService reminderService,
                                LogService logService, TaskAgentExecutor taskAgentExecutor,
                                TokenService tokenService, ISysUserService sysUserService) {
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.logService = logService;
        this.taskAgentExecutor = taskAgentExecutor;
        this.tokenService = tokenService;
        this.sysUserService = sysUserService;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private LoginUser getLoginUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return null;
        return tokenService.getLoginUser(request);
    }

    private Long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    private String getCurrentUserName() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) return null;
        SysUser user = loginUser.getUser();
        return user != null ? user.getUserName() : null;
    }

    private boolean isCurrentUserAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && SysUser.isAdmin(loginUser.getUserId());
    }

    @GetMapping("/api/tasks")
    @ResponseBody
    public Map<String, Object> getTasks(@RequestParam(required = false) Long kbId) {
        Long userId = getCurrentUserId();
        List<TaskItem> tasks = taskService.getAllTasksForUser(userId, isCurrentUserAdmin());
        Map<Long, Integer> queueInfo = taskAgentExecutor.getQueueInfo();
        Map<Long, String> userNameCache = new java.util.HashMap<>();
        List<Map<String, Object>> result = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.id);
            m.put("title", t.title);
            m.put("description", t.description);
            m.put("status", t.status);
            m.put("priority", t.priority);
            m.put("createdAt", t.createdAt);
            m.put("updatedAt", t.updatedAt);
            m.put("dueDate", t.dueDate);
            m.put("action", t.action);
            m.put("actionPrompt", t.actionPrompt);
            m.put("scheduledStart", t.scheduledStart);
            m.put("createdBy", t.createdBy);
            m.put("creatorName", resolveUserName(t.createdBy, userNameCache));
            m.put("kbId", t.kbId);
            m.put("scheduleType", t.scheduleType);
            m.put("cycleType", t.cycleType);
            m.put("cycleValue", t.cycleValue);
            m.put("cycleTime", t.cycleTime);
            m.put("cycleEnd", t.cycleEnd);
            m.put("lastCycleRun", t.lastCycleRun);
            Integer pos = queueInfo.get(t.id);
            m.put("queuePosition", pos);
            if (pos != null) {
                m.put("queueStatus", "QUEUED");
            } else if (t.createdBy != null && taskService.hasActiveExecution(t.id)) {
                m.put("queueStatus", "RUNNING");
            }
            return m;
        }).toList();
        return Map.of("ok", true, "tasks", result);
    }

    private String resolveUserName(Long userId, Map<Long, String> cache) {
        if (userId == null) return null;
        return cache.computeIfAbsent(userId, id -> {
            try {
                SysUser u = sysUserService.selectUserById(id);
                return u != null && u.getUserName() != null ? u.getUserName() : String.valueOf(id);
            } catch (Exception e) {
                return String.valueOf(id);
            }
        });
    }

    @GetMapping("/api/tasks/queue")
    @ResponseBody
    public Map<String, Object> getQueueInfo() {
        return Map.of("ok", true, "queue", taskAgentExecutor.getQueueInfo(), "size", taskAgentExecutor.getQueueSize());
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
            @RequestParam(required = false, defaultValue = "") String actionPrompt,
            @RequestParam(required = false, defaultValue = "") String scheduledStart,
            @RequestParam(required = false, defaultValue = "") String scheduleType,
            @RequestParam(required = false, defaultValue = "") String cycleType,
            @RequestParam(required = false, defaultValue = "") String cycleValue,
            @RequestParam(required = false, defaultValue = "") String cycleTime,
            @RequestParam(required = false, defaultValue = "") String cycleEnd) {
        try {
            Long userId = getCurrentUserId();
            TaskItem task = taskService.addTask(title, description, priority, dueDate, kbId, action, actionPrompt,
                    scheduledStart, userId, scheduleType, cycleType, cycleValue, cycleTime, cycleEnd);
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
            @RequestParam Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actionPrompt,
            @RequestParam(required = false) String scheduledStart,
            @RequestParam(required = false) String scheduleType,
            @RequestParam(required = false) String cycleType,
            @RequestParam(required = false) String cycleValue,
            @RequestParam(required = false) String cycleTime,
            @RequestParam(required = false) String cycleEnd) {
        try {
            TaskItem task = taskService.updateTask(id, title, description, status, priority, dueDate, kbId,
                    action, actionPrompt, scheduledStart, scheduleType, cycleType, cycleValue, cycleTime, cycleEnd);
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
            @RequestParam Long id) {
        try {
            taskAgentExecutor.cancelQueued(id);
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
            @RequestParam Long id) {
        try {
            TaskItem task = taskService.getAllTasks().stream()
                    .filter(x -> id.equals(x.id))
                    .findFirst().orElse(null);
            if (task == null) {
                return Map.of("ok", false, "error", "任务不存在");
            }
            if (taskService.hasActiveExecution(id)) {
                return Map.of("ok", false, "error", "该任务已在排队或执行中，请勿重复触发");
            }
            Map<String, Object> queueInfo = taskAgentExecutor.enqueue(task, kbId != null ? kbId : task.kbId, "manual", getCurrentUserName());
            logService.add("任务中心", "成功", "手动触发 AI 执行: " + task.title);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("executionId", queueInfo.get("executionId"));
            result.put("position", queueInfo.get("position"));
            return result;
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/tasks/cancel-queue")
    @ResponseBody
    public Map<String, Object> cancelQueuedTask(@RequestParam Long id) {
        try {
            boolean ok = taskAgentExecutor.cancelQueued(id);
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/api/tasks/executions")
    @ResponseBody
    public Map<String, Object> getExecutions(@RequestParam(required = false) Long taskId,
                                             @RequestParam(required = false, defaultValue = "1") int page,
                                             @RequestParam(required = false, defaultValue = "10") int pageSize) {
        if (taskId != null) {
            return Map.of("ok", true, "executions", taskService.getTaskExecutions(taskId));
        }
        return taskService.getExecutionsPage(page, pageSize, getCurrentUserId(), isCurrentUserAdmin());
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
            @RequestParam Long id,
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
            @RequestParam Long id) {
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
            @RequestParam Long id) {
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
            @RequestParam Long id) {
        try {
            List<Reminder> reminders = reminderService.getAllReminders();
            Reminder r = reminders.stream().filter(x -> id.equals(x.id)).findFirst().orElse(null);
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
