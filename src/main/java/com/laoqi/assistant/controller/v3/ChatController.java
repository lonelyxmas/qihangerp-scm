package com.laoqi.assistant.controller.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.entity.LlmProfileEntity;
import com.laoqi.assistant.entity.MessageEntity;
import com.laoqi.assistant.entity.SessionEntity;
import com.laoqi.assistant.service.*;
import com.laoqi.assistant.service.db.MessageDbService;
import com.laoqi.assistant.service.db.SessionDbService;
import com.laoqi.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/v3")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
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
    private final LogService logService;

    public ChatController(KnowledgeBaseService kbService,
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

//    @GetMapping("/ai")
//    public String aiPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
//        model.put("currentNav", "ai");
//        List<KnowledgeBaseEntity> kbList = kbService.getAll();
//        model.put("kbList", kbList);
//
//        if (!kbList.isEmpty()) {
//            model.put("defaultKbId", kbList.get(0).getId());
//        }
//
//        if (kbId == null && !kbList.isEmpty()) {
//            kbId = kbList.get(0).getId();
//        }
//
//        if (kbId != null) {
//            KnowledgeBaseEntity kb = kbService.getById(kbId);
//            if (kb != null) {
//                model.put("selectedKb", kb);
//            }
//        }
//
//        return "3.0/ai";
//    }

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