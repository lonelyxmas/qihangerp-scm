package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.model.ReminderData.Reminder;
import cn.qihang.ai.assistant.model.TaskData.TaskItem;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.TokenService;
import cn.qihang.ai.assistant.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RemindersApiController {

    private final TaskService taskService;
    private final ReminderService reminderService;
    private final LogService logService;
    private final TaskAgentExecutor taskAgentExecutor;
    private final TokenService tokenService;
    private final ISysUserService sysUserService;

    public RemindersApiController(TaskService taskService, ReminderService reminderService,
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



    @GetMapping("/api/reminders")
    @ResponseBody
    public List<Map<String, Object>> getReminders(@RequestParam(required = false) Long kbId) {
        return reminderService.getAllReminders().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
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
