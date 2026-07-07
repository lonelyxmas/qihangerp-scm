package com.laoqi.assistant.controller.v3;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.model.ReminderData.Reminder;
import com.laoqi.assistant.model.TaskData.TaskItem;
import com.laoqi.assistant.service.KnowledgeBaseService;
import com.laoqi.assistant.service.ReminderService;
import com.laoqi.assistant.service.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/v3")
public class PlannerController {

    private final KnowledgeBaseService kbService;
    private final TaskService taskService;
    private final ReminderService reminderService;

    public PlannerController(KnowledgeBaseService kbService,
                             TaskService taskService,
                             ReminderService reminderService) {
        this.kbService = kbService;
        this.taskService = taskService;
        this.reminderService = reminderService;
    }

    @GetMapping("/planner")
    public String plannerPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "planner");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/planner";
    }

    @ResponseBody
    @GetMapping("/api/tasks")
    public Map<String, Object> getTasks(@RequestParam(required = false) Long kbId) {
        List<TaskItem> tasks;
        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
            tasks = taskService.getAllTasks(kb.getNotesDir());
        } else {
            tasks = taskService.getAllTasks();
        }
        return Map.of("ok", true, "tasks", tasks);
    }

    @ResponseBody
    @PostMapping("/api/tasks/add")
    public Map<String, Object> addTask(@RequestParam(required = false) Long kbId,
                                       @RequestParam String title,
                                       @RequestParam(required = false, defaultValue = "") String description,
                                       @RequestParam(required = false, defaultValue = "mid") String priority,
                                       @RequestParam(required = false, defaultValue = "") String dueDate) {
        try {
            TaskItem task;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                task = taskService.addTask(kb.getNotesDir(), title, description, priority, dueDate);
            } else {
                task = taskService.addTask(title, description, priority, dueDate);
            }
            return Map.of("ok", true, "task", task);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/tasks/update")
    public Map<String, Object> updateTask(@RequestParam(required = false) Long kbId,
                                          @RequestParam String taskId,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String description,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String priority,
                                          @RequestParam(required = false) String dueDate) {
        try {
            TaskItem task;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                task = taskService.updateTask(kb.getNotesDir(), taskId, title, description, status, priority, dueDate);
            } else {
                task = taskService.updateTask(taskId, title, description, status, priority, dueDate);
            }
            if (task == null) return Map.of("ok", false, "error", "任务不存在");
            return Map.of("ok", true, "task", task);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/tasks/delete")
    public Map<String, Object> deleteTask(@RequestParam(required = false) Long kbId, @RequestParam String taskId) {
        try {
            boolean ok;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                ok = taskService.deleteTask(kb.getNotesDir(), taskId);
            } else {
                ok = taskService.deleteTask(taskId);
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/api/reminders")
    public Map<String, Object> getReminders(@RequestParam(required = false) Long kbId) {
        List<Reminder> reminders;
        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
            reminders = reminderService.getAllReminders(kb.getNotesDir());
        } else {
            reminders = reminderService.getAllReminders();
        }
        return Map.of("ok", true, "reminders", reminders);
    }

    @ResponseBody
    @PostMapping("/api/reminders/add")
    public Map<String, Object> addReminder(@RequestParam(required = false) Long kbId,
                                           @RequestParam String name,
                                           @RequestParam(required = false, defaultValue = "") String message,
                                           @RequestParam String type,
                                           @RequestParam(required = false, defaultValue = "09:00") String time,
                                           @RequestParam(required = false, defaultValue = "") String date,
                                           @RequestParam(required = false, defaultValue = "") String dayOfWeek,
                                           @RequestParam(required = false, defaultValue = "") String dayOfMonth,
                                           @RequestParam(required = false, defaultValue = "") String monthDay) {
        try {
            Reminder r;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                r = reminderService.addReminder(kb.getNotesDir(), name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay);
            } else {
                r = reminderService.addReminder(name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay);
            }
            return Map.of("ok", true, "reminder", r);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/reminders/update")
    public Map<String, Object> updateReminder(@RequestParam(required = false) Long kbId,
                                              @RequestParam String reminderId,
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
            boolean ok;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                ok = reminderService.updateReminder(kb.getNotesDir(), reminderId, name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, enabled);
            } else {
                ok = reminderService.updateReminder(reminderId, name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, enabled);
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/reminders/delete")
    public Map<String, Object> deleteReminder(@RequestParam(required = false) Long kbId, @RequestParam String reminderId) {
        try {
            boolean ok;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                ok = reminderService.deleteReminder(kb.getNotesDir(), reminderId);
            } else {
                ok = reminderService.deleteReminder(reminderId);
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/reminders/toggle")
    public Map<String, Object> toggleReminder(@RequestParam(required = false) Long kbId, @RequestParam String reminderId) {
        try {
            boolean ok;
            if (kbId != null) {
                KnowledgeBaseEntity kb = kbService.getById(kbId);
                if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");
                ok = reminderService.toggleReminder(kb.getNotesDir(), reminderId);
            } else {
                ok = reminderService.toggleReminder(reminderId);
            }
            return Map.of("ok", ok);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}