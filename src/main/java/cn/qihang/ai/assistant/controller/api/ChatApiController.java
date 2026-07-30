package cn.qihang.ai.assistant.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.entity.MessageEntity;
import cn.qihang.ai.assistant.entity.SessionEntity;
import cn.qihang.ai.assistant.service.*;
import cn.qihang.ai.assistant.service.db.MessageDbService;
import cn.qihang.ai.assistant.service.db.SessionDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final Logger log = LoggerFactory.getLogger(ChatApiController.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ExecutorService chatExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-sse");
        t.setDaemon(true);
        return t;
    });

    private final KbBaseService kbService;
    private final SessionDbService sessionDbService;
    private final MessageDbService messageDbService;
    private final SessionService sessionService;
    private final LlmService llmService;
    private final NoteAssistantService noteAssistantService;
    private final LlmConfigResolver llmConfigResolver;
    private final LogService logService;

    public ChatApiController(KbBaseService kbService,
                            SessionDbService sessionDbService,
                            MessageDbService messageDbService,
                            SessionService sessionService,
                            LlmService llmService,
                            NoteAssistantService noteAssistantService,
                            LlmConfigResolver llmConfigResolver,
                            LogService logService) {
        this.kbService = kbService;
        this.sessionDbService = sessionDbService;
        this.messageDbService = messageDbService;
        this.sessionService = sessionService;
        this.llmService = llmService;
        this.noteAssistantService = noteAssistantService;
        this.llmConfigResolver = llmConfigResolver;
        this.logService = logService;
    }

    @GetMapping("/kbs")
    public ResponseEntity<Map<String, Object>> listKbs() {
        List<KbBaseEntity> kbs = kbService.getAccessibleKbs();
        List<Map<String, Object>> result = new ArrayList<>();
        for (KbBaseEntity kb : kbs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kb.getId());
            item.put("name", kb.getName());
            result.add(item);
        }
        return ResponseEntity.ok(Map.of("ok", true, "data", result));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> listSessions() {
        List<SessionEntity> sessions = sessionDbService.listBySourceOrderByUpdate("web");
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionEntity s : sessions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("title", s.getTitle());
            item.put("mode", s.getMode());
            item.put("updatedAt", s.getUpdatedAt());
            
            result.add(item);
        }
        return ResponseEntity.ok(Map.of("ok", true, "data", result));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestParam(required = false) String title,
                                                             @RequestParam(required = false, defaultValue = "knowledge") String mode) {
        String id = UUID.randomUUID().toString().substring(0, 12);
        String now = TimeUtil.nowStr();
        SessionEntity se = new SessionEntity();
        se.setId(id);
        se.setSource("web");
        se.setTitle(title != null && !title.isBlank() ? title : "新对话");
        se.setMode(mode != null ? mode : "knowledge");
        se.setCreatedAt(now);
        se.setUpdatedAt(now);
        sessionDbService.save(se);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("id", id);
        result.put("title", se.getTitle());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels() {
        List<cn.qihang.ai.assistant.entity.LlmProfileEntity> chatModels = llmConfigResolver.getAllProfiles()
                .stream()
                .filter(p -> !cn.qihang.ai.assistant.entity.LlmProfileEntity.TYPE_EMBEDDING.equals(p.getModelType()))
                .toList();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (var model : chatModels) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", model.getName());
            item.put("modelType", model.getModelType());
            result.add(item);
        }
        
        String defaultModel = null;
        var defaultProfile = llmConfigResolver.getDefaultProfile();
        if (defaultProfile != null) {
            defaultModel = defaultProfile.getName();
        }
        
        return ResponseEntity.ok(Map.of("ok", true, "data", result, "defaultModel", defaultModel != null ? defaultModel : ""));
    }

    @PostMapping("/send")
    public SseEmitter sendChat(@RequestParam String message,
                               @RequestParam(required = false) Long kbId,
                               @RequestParam(required = false, defaultValue = "knowledge") String mode,
                               @RequestParam(required = false, defaultValue = "") String modelName,
                               @RequestParam(required = false) String sessionId) {
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
        final String providedSessionId = sessionId;

        final boolean[] emitterDone = {false};
        emitter.onCompletion(() -> emitterDone[0] = true);
        emitter.onTimeout(() -> emitterDone[0] = true);
        emitter.onError(e -> emitterDone[0] = true);

        chatExecutor.execute(() -> {
            try {
                if (emitterDone[0]) return;

                String actualSessionId = getOrCreateSession(providedSessionId, finalKbId, mode);

                if (emitterDone[0]) return;
                emitter.send(SseEmitter.event().data(mapper.writeValueAsString(
                        Map.of("type", "session", "sessionId", actualSessionId))));

                if (emitterDone[0]) return;
                sendStatus(emitter, mode, "正在处理...");

                sessionService.saveMessage(actualSessionId, "user", finalMessage, mode, "web", finalKbId);

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
                }, "chat-heartbeat");
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
                }, "chat-thinking-status");
                thinkingStatus.setDaemon(true);
                thinkingStatus.start();

                StringBuilder replyBuffer = new StringBuilder();
                noteAssistantService.streamChat(actualSessionId, finalMessage, mode, finalKbId, modelName, chunk -> {
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
                sessionService.saveMessage(actualSessionId, "assistant", replyText, mode, "web");
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

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(@RequestParam(required = false) Long kbId,
                                                           @RequestParam(required = false) String sessionId,
                                                           @RequestParam(defaultValue = "0") int offset,
                                                           @RequestParam(defaultValue = "60") int limit) {
        List<MessageEntity> msgs;
        long total;

        if (sessionId != null && !sessionId.isBlank()) {
            msgs = messageDbService.listBySession(sessionId);
            total = msgs.size();
        } else if (kbId != null) {
            msgs = messageDbService.listByKb(kbId, offset, limit);
            total = messageDbService.countByKb(kbId);
        } else {
            SessionEntity globalSession = sessionDbService.listBySourceOrderByUpdate("web").stream()
                    .findFirst().orElse(null);
            if (globalSession != null) {
                msgs = messageDbService.listBySession(globalSession.getId());
                total = msgs.size();
            } else {
                return ResponseEntity.ok(Map.of("ok", true, "messages", List.of(), "total", 0));
            }
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (MessageEntity me : msgs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", me.getRole());
            m.put("content", me.getContent());
            m.put("time", me.getCreatedAt());
            m.put("mode", me.getMode());
            messages.add(m);
        }

        return ResponseEntity.ok(Map.of("ok", true, "messages", messages, "total", total));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearChat(@RequestParam(required = false) Long kbId,
                                                         @RequestParam(required = false) String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionService.deleteSession(sessionId);
            logService.add("对话", "清空", "清空会话(session=" + sessionId + ")的聊天记录");
        } else if (kbId != null) {
            sessionService.deleteMessagesByKb(kbId);
            logService.add("对话", "清空", "清空笔记库(KB=" + kbId + ")的聊天记录");
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(@RequestParam Long kbId,
                                                               @RequestParam String q,
                                                               @RequestParam(defaultValue = "30") int limit) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Map.of("ok", true, "messages", List.of()));
        }
        List<MessageEntity> msgs = messageDbService.searchByKb(kbId, q, limit);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (MessageEntity me : msgs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", me.getRole());
            m.put("content", me.getContent());
            m.put("time", me.getCreatedAt());
            m.put("mode", me.getMode());
            messages.add(m);
        }
        return ResponseEntity.ok(Map.of("ok", true, "messages", messages, "query", q));
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportMessages(@RequestParam Long kbId) {
        List<MessageEntity> msgs = messageDbService.listByKb(kbId, 0, 99999);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (MessageEntity me : msgs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", me.getRole());
            m.put("content", me.getContent());
            m.put("time", me.getCreatedAt());
            messages.add(m);
        }

        KbBaseEntity kb = kbService.getById(kbId);
        String title = (kb != null ? kb.getName() : "知识库") + "对话导出";
        String date = TimeUtil.todayStr();
        StringBuilder sb = new StringBuilder();
        sb.append("---\ntitle: ").append(title).append("\ndate: ").append(date).append("\n---\n\n");

        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> m = messages.get(i);
            sb.append("**").append("user".equals(m.get("role")) ? "👤 用户" : "🤖 AI").append("**\n");
            sb.append(m.get("content")).append("\n\n");
        }

        return ResponseEntity.ok(Map.of("ok", true, "title", title, "content", sb.toString()));
    }
    private Long parseMentionedKb(String message) {
        Pattern pattern = Pattern.compile("@(\\S+)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String kbName = matcher.group(1);
List<KbBaseEntity> kbs = kbService.getAccessibleKbs();
            for (KbBaseEntity kb : kbs) {
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

    private String getOrCreateSession(String providedSessionId, Long kbId, String mode) {
        if (providedSessionId != null && !providedSessionId.isBlank()) {
            SessionEntity existing = sessionDbService.getById(providedSessionId);
            if (existing != null) {
                existing.setUpdatedAt(TimeUtil.nowStr());
                sessionDbService.updateById(existing);
                return existing.getId();
            }
        }

        SessionEntity latest = sessionDbService.listBySourceOrderByUpdate("web").stream()
                .findFirst().orElse(null);
        if (latest != null) {
            latest.setUpdatedAt(TimeUtil.nowStr());
            sessionDbService.updateById(latest);
            return latest.getId();
        }

        String id = UUID.randomUUID().toString().substring(0, 12);
        String now = TimeUtil.nowStr();
        SessionEntity se = new SessionEntity();
        se.setId(id);
        se.setSource("web");
        se.setTitle("新对话");
        se.setMode(mode != null ? mode : "knowledge");
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