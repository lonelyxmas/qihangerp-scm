package com.laoqi.assistant.controller.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.entity.LlmProfileEntity;
import com.laoqi.assistant.entity.MessageEntity;
import com.laoqi.assistant.entity.SessionEntity;
import com.laoqi.assistant.model.TaskData.TaskItem;
import com.laoqi.assistant.model.ReminderData.Reminder;
import com.laoqi.assistant.service.*;
import com.laoqi.assistant.service.db.MessageDbService;
import com.laoqi.assistant.service.db.SessionDbService;
import com.laoqi.assistant.util.FileUtil;
import com.laoqi.assistant.util.MarkdownUtil;
import com.laoqi.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/v3")
public class V3Controller {

    private static final Logger log = LoggerFactory.getLogger(V3Controller.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ExecutorService chatExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-sse-v3");
        t.setDaemon(true);
        return t;
    });

    private final KnowledgeBaseService kbService;
    private final SessionDbService sessionDbService;
    private final MessageDbService messageDbService;
    private final SessionService sessionService;
    private final LlmService llmService;
    private final NoteAssistantService noteAssistantService;
    private final LlmConfigResolver llmConfigResolver;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final NoteIndexService noteIndexService;
    private final LogService logService;

    public V3Controller(KnowledgeBaseService kbService,
                        SessionDbService sessionDbService,
                        MessageDbService messageDbService,
                        SessionService sessionService,
                        LlmService llmService,
                        NoteAssistantService noteAssistantService,
                        LlmConfigResolver llmConfigResolver,
                        TaskService taskService,
                        ReminderService reminderService,
                        NoteIndexService noteIndexService,
                        LogService logService) {
        this.kbService = kbService;
        this.sessionDbService = sessionDbService;
        this.messageDbService = messageDbService;
        this.sessionService = sessionService;
        this.llmService = llmService;
        this.noteAssistantService = noteAssistantService;
        this.llmConfigResolver = llmConfigResolver;
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.noteIndexService = noteIndexService;
        this.logService = logService;
    }

    // ========== 页面路由 ==========

    @GetMapping("")
    public String index(Map<String, Object> model) {
        return chatPage(model);
    }

    @GetMapping("/chat")
    public String chatPage(Map<String, Object> model) {
        model.put("currentNav", "chat");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        model.put("hasKb", !kbList.isEmpty());
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        List<LlmProfileEntity> chatModels = llmConfigResolver.getAllProfiles()
                .stream()
                .filter(p -> !LlmProfileEntity.TYPE_EMBEDDING.equals(p.getModelType()))
                .collect(Collectors.toList());
        model.put("chatModels", chatModels);
        LlmProfileEntity defaultProfile = llmConfigResolver.getDefaultProfile();
        model.put("defaultModel", defaultProfile != null ? defaultProfile.getName() : "");

        return "3.0/chat";
    }

    @GetMapping("/ai")
    public String aiPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "ai");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId == null && !kbList.isEmpty()) {
            kbId = kbList.get(0).getId();
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
                try {
                    var stats = noteIndexService.getIndexStats(kb.getId());
                    model.put("fileCount", stats.fileCount());
                    model.put("indexCount", stats.chunkCount());
                } catch (Exception e) {
                    model.put("fileCount", 0);
                    model.put("indexCount", 0);
                }
                try {
                    model.put("totalMessages", messageDbService.countByKb(kbId));
                } catch (Exception e) {
                    model.put("totalMessages", 0);
                }
            }
        }

        return "3.0/ai";
    }

    @GetMapping("/notes")
    public String notesPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "notes");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId == null && !kbList.isEmpty()) {
            kbId = kbList.get(0).getId();
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/notes";
    }

    @GetMapping("/data")
    public String dataPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "data");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId == null && !kbList.isEmpty()) {
            kbId = kbList.get(0).getId();
        }

        if (kbId != null) {
            KnowledgeBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/data";
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

    @GetMapping("/tools")
    public String toolsPage(Map<String, Object> model) {
        model.put("currentNav", "tools");
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        return "3.0/tools";
    }

    @GetMapping("/data/module/{moduleId}")
    public String dataModulePage(@PathVariable String moduleId, Map<String, Object> model) {
        model.put("currentNav", "data");
        model.put("moduleId", moduleId);
        List<KnowledgeBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        return "3.0/data-module";
    }

    // ========== API: 获取笔记库列表 ==========

    @ResponseBody
    @GetMapping("/api/kbs")
    public Map<String, Object> listKbs() {
        List<KnowledgeBaseEntity> kbs = kbService.getAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (KnowledgeBaseEntity kb : kbs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kb.getId());
            item.put("name", kb.getName());
            item.put("notesDir", kb.getNotesDir());
            result.add(item);
        }
        return Map.of("ok", true, "data", result);
    }

    // ========== API: 发送消息（支持@笔记库） ==========

    @PostMapping("/api/chat/send")
    @ResponseBody
    public SseEmitter sendChat(@RequestParam String message,
                               @RequestParam(required = false) Long kbId,
                               @RequestParam(required = false, defaultValue = "knowledge") String mode,
                               @RequestParam(required = false, defaultValue = "") String modelName) {
        SseEmitter emitter = new SseEmitter(300_000L);

        String cleanMessage = message;
        Long resolvedKbId = kbId;

        if (resolvedKbId == null) {
            resolvedKbId = parseMentionedKb(message);
            if (resolvedKbId != null) {
                cleanMessage = removeMentions(message);
            }
        }

        final Long finalKbId = resolvedKbId;
        final String finalMessage = cleanMessage;

        final boolean[] emitterDone = {false};
        emitter.onCompletion(() -> emitterDone[0] = true);
        emitter.onTimeout(() -> emitterDone[0] = true);
        emitter.onError(e -> emitterDone[0] = true);

        chatExecutor.execute(() -> {
            try {
                if (emitterDone[0]) return;

                if (finalKbId == null) {
                    String hint = "请先选择一个笔记库，或在问题中使用 @笔记库名 指定要搜索的笔记库。\n\n" +
                            "例如：'@工作笔记 查一下项目进展'\n\n" +
                            "可用笔记库：";
                    List<KnowledgeBaseEntity> kbList = kbService.getAll();
                    if (kbList.isEmpty()) {
                        hint += "\n- 暂无笔记库，请先到配置页添加";
                    } else {
                        for (KnowledgeBaseEntity kb : kbList) {
                            hint += "\n- @" + kb.getName();
                        }
                    }
                    
                    emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                            Map.of("type", "text", "content", hint, "mode", mode))));
                    sendDone(emitter, mode);
                    return;
                }

                String sessionId = getOrCreateSession(finalKbId, mode);

                if (emitterDone[0]) return;
                emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                        Map.of("type", "session", "sessionId", sessionId))));

                if (emitterDone[0]) return;
                sendStatus(emitter, mode, "正在处理...");

                sessionService.saveMessage(sessionId, "user", finalMessage, mode, "web");

                if (!llmService.isAvailable()) {
                    throw new IllegalStateException("LLM API Key 未配置，请在配置页填写");
                }

                final boolean[] heartbeatDone = {false};
                Thread heartbeat = new Thread(() -> {
                    while (!heartbeatDone[0] && !emitterDone[0]) {
                        try {
                            Thread.sleep(5000);
                            if (!heartbeatDone[0] && !emitterDone[0]) {
                                emitter.send(SseEmitter.event()
                                        .data(mapper.writeValueAsString(Map.of("type", "heartbeat"))));
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }
                }, "chat-heartbeat-v3");
                heartbeat.setDaemon(true);
                heartbeat.start();

                final boolean[] firstChunkArrived = {false};
                final long[] lastStatusTime = {System.currentTimeMillis()};
                Thread thinkingStatus = new Thread(() -> {
                    while (!firstChunkArrived[0] && !emitterDone[0]) {
                        try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
                        if (!firstChunkArrived[0] && !emitterDone[0]) {
                            long elapsed = System.currentTimeMillis() - lastStatusTime[0];
                            if (elapsed >= 3000) {
                                String msg;
                                if (elapsed < 6000) msg = "⏳ 正在分析获取到的信息...";
                                else if (elapsed < 10000) msg = "✍️ AI 正在组织回复...";
                                else if (elapsed < 15000) msg = "📝 即将完成...";
                                else msg = "⏳ 处理中，请稍候...";
                                sendStatus(emitter, mode, msg);
                            }
                        }
                    }
                }, "chat-thinking-status-v3");
                thinkingStatus.setDaemon(true);
                thinkingStatus.start();

                StringBuilder replyBuffer = new StringBuilder();
                noteAssistantService.streamChat(sessionId, finalMessage, mode, finalKbId, modelName, chunk -> {
                    if (emitterDone[0]) return;
                    firstChunkArrived[0] = true;
                    replyBuffer.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                                Map.of("type", "text", "content", chunk, "mode", mode))));
                    } catch (Exception e) {
                        emitterDone[0] = true;
                    }
                }, status -> {
                    if (!emitterDone[0]) {
                        lastStatusTime[0] = System.currentTimeMillis();
                        sendStatus(emitter, mode, status);
                    }
                });

                heartbeatDone[0] = true;
                if (emitterDone[0]) return;
                String replyText = replyBuffer.toString();
                sessionService.saveMessage(sessionId, "assistant", replyText, mode, "web");
                sendDone(emitter, mode);

            } catch (Exception e) {
                log.error("对话请求处理失败", e);
                try {
                    sendError(emitter, resolveErrorMessage(e));
                } catch (Exception ex) {
                    try { emitter.completeWithError(ex); } catch (Exception ignored) {}
                }
            }
        });

        return emitter;
    }

    // ========== API: 加载对话历史 ==========

    @ResponseBody
    @GetMapping("/api/chat/messages")
    public Map<String, Object> getMessages(@RequestParam(required = false) Long kbId,
                                           @RequestParam(defaultValue = "0") int offset,
                                           @RequestParam(defaultValue = "60") int limit) {
        if (kbId == null) {
            return Map.of("ok", true, "messages", List.of(), "total", 0);
        }
        List<MessageEntity> msgs = messageDbService.listByKb(kbId, offset, limit);
        long total = messageDbService.countByKb(kbId);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = msgs.size() - 1; i >= 0; i--) {
            MessageEntity me = msgs.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", me.getRole());
            m.put("content", me.getContent());
            m.put("time", me.getCreatedAt());
            m.put("mode", me.getMode());
            messages.add(m);
        }

        return Map.of("ok", true, "messages", messages, "total", total);
    }

    // ========== API: 清空对话 ==========

    @ResponseBody
    @DeleteMapping("/api/chat/clear")
    public Map<String, Object> clearChat(@RequestParam Long kbId) {
        List<SessionEntity> sessions = sessionDbService.listByKb(kbId);
        for (SessionEntity se : sessions) {
            sessionService.deleteSession(se.getId());
        }
        logService.add("对话", "清空", "清空笔记库(KB=" + kbId + ")的聊天记录");
        return Map.of("ok", true);
    }

    // ========== API: 笔记相关 ==========

    @ResponseBody
    @GetMapping("/api/notes/tree")
    public Map<String, Object> getNotesTree(@RequestParam Long kbId) {
        KnowledgeBaseEntity kb = kbService.getById(kbId);
        if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");

        Path base = Paths.get(kb.getNotesDir());
        if (kb.getNotesDir() == null || kb.getNotesDir().isBlank()) {
            return Map.of("ok", true, "tree", Map.of());
        }

        Map<String, Object> tree = buildFileTree(base, base);
        return Map.of("ok", true, "tree", tree);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFileTree(Path root, Path current) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> dirs = new ArrayList<>();
        List<Map<String, String>> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            List<Path> entries = new ArrayList<>();
            stream.forEach(entries::add);

            entries.sort((a, b) -> {
                boolean aIsDir = Files.isDirectory(a);
                boolean bIsDir = Files.isDirectory(b);
                if (aIsDir != bIsDir) return aIsDir ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            });

            for (Path entry : entries) {
                String name = entry.getFileName().toString();

                if (name.startsWith(".") || name.equals("__pycache__")) continue;

                String relativePath = root.relativize(entry).toString().replace("\\", "/");

                if (Files.isDirectory(entry)) {
                    if (name.equals("AI") || name.equals(".git") || name.equals(".obsidian")) continue;

                    Map<String, Object> dir = new LinkedHashMap<>();
                    dir.put("name", name);
                    dir.put("path", relativePath);
                    dir.put("children", buildFileTree(root, entry));
                    dirs.add(dir);
                } else if (name.endsWith(".md")) {
                    Map<String, String> file = new LinkedHashMap<>();
                    file.put("name", name);
                    file.put("path", relativePath);
                    files.add(file);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list directory: {}", current);
        }

        result.put("dirs", dirs);
        result.put("files", files);
        return result;
    }

    @ResponseBody
    @GetMapping("/api/notes/read")
    public Map<String, Object> readNote(@RequestParam Long kbId, @RequestParam String path) {
        KnowledgeBaseEntity kb = kbService.getById(kbId);
        if (kb == null) return Map.of("ok", false, "error", "笔记库不存在");

        Path base = Paths.get(kb.getNotesDir());
        Path file = safeResolve(base, path);

        if (!Files.isRegularFile(file)) {
            return Map.of("ok", false, "error", "文件不存在");
        }

        try {
            String content = FileUtil.readText(file);
            content = MarkdownUtil.stripFrontmatter(content);
            return Map.of("ok", true, "content", content);
        } catch (Exception e) {
            return Map.of("ok", false, "error", "读取失败");
        }
    }

    private Path safeResolve(Path base, String rel) {
        Path normalized = base.normalize();
        Path resolved = normalized.resolve(rel != null ? rel : "").normalize();
        if (!resolved.startsWith(normalized)) return normalized;
        return resolved;
    }

    // ========== API: 任务相关 ==========

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

    // ========== API: 提醒相关 ==========

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

    // ========== 辅助方法 ==========

    private Long parseMentionedKb(String message) {
        Pattern pattern = Pattern.compile("@(\\S+)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String kbName = matcher.group(1);
            List<KnowledgeBaseEntity> kbs = kbService.getAll();
            for (KnowledgeBaseEntity kb : kbs) {
                if (kb.getName().equals(kbName) || kb.getName().contains(kbName)) {
                    return kb.getId();
                }
            }
        }
        return null;
    }

    private String removeMentions(String message) {
        return message.replaceAll("@\\S+", "").trim();
    }

    private String getOrCreateSession(Long kbId, String mode) {
        SessionEntity latest = sessionDbService.findLatestByKb(kbId);
        if (latest != null) return latest.getId();

        String id = UUID.randomUUID().toString().substring(0, 12);
        String now = TimeUtil.nowStr();
        SessionEntity se = new SessionEntity();
        se.setId(id);
        se.setSource("web");
        se.setTitle("连续对话");
        se.setMode(mode != null ? mode : "knowledge");
        se.setKbId(kbId);
        se.setCreatedAt(now);
        se.setUpdatedAt(now);
        sessionDbService.save(se);
        return id;
    }

    private void sendStatus(SseEmitter emitter, String mode, String text) {
        try {
            emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                    Map.of("type", "status", "content", text, "mode", mode))));
        } catch (Exception e) {
            log.warn("发送状态消息失败", e);
        }
    }

    private void sendDone(SseEmitter emitter, String mode) {
        try {
            emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                    Map.of("type", "done", "mode", mode))));
            emitter.complete();
        } catch (Exception e) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }

    private void sendError(SseEmitter emitter, String error) {
        try {
            emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                    Map.of("type", "error", "content", error))));
            emitter.complete();
        } catch (Exception e) {
            try { emitter.completeWithError(new RuntimeException(error)); } catch (Exception ignored) {}
        }
    }

    private String resolveErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("Insufficient Balance") || msg.contains("insufficient_balance")) {
                return "API 余额不足，请登录平台充值后重试。";
            }
            if (msg.contains("Rate limit") || msg.contains("rate_limit")) {
                return "请求过于频繁，请稍后再试。";
            }
            if (msg.contains("invalid_api_key") || msg.contains("Incorrect API key")) {
                return "API Key 无效，请在配置页检查并更新。";
            }
            if (msg.contains("context_length_exceeded") || msg.contains("maximum context length")) {
                return "对话过长，请开启新对话。";
            }
        }
        return "AI 服务调用失败: " + (msg != null ? msg : "未知错误");
    }
}