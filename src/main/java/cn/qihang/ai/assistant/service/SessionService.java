package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.MessageEntity;
import cn.qihang.ai.assistant.entity.SessionEntity;
import cn.qihang.ai.assistant.service.db.MessageDbService;
import cn.qihang.ai.assistant.service.db.SessionDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private static final int DEFAULT_FALLBACK_TURNS = 10;

    private final SessionDbService sessionDbService;
    private final MessageDbService messageDbService;

    public SessionService(SessionDbService sessionDbService,
                          MessageDbService messageDbService) {
        this.sessionDbService = sessionDbService;
        this.messageDbService = messageDbService;
    }

// ========== Session CRUD ==========

    public SessionEntity getSession(String id) {
        return sessionDbService.getById(id);
    }

    public SessionEntity getOrCreateWebSession(String sessionId) {
        SessionEntity se = sessionDbService.getById(sessionId);
        if (se != null) return se;

        String now = TimeUtil.nowStr();
        se = new SessionEntity();
        se.setId(sessionId);
        se.setSource("web");
        se.setTitle("新对话");
        se.setMode("knowledge");
        se.setCreatedAt(now);
        se.setUpdatedAt(now);
        sessionDbService.save(se);
        return se;
    }

    public SessionEntity getOrCreateFeishuSession(String userKey, String chatId, String chatType) {
        SessionEntity se = sessionDbService.getById(userKey);
        if (se != null) return se;

        String now = TimeUtil.nowStr();
        se = new SessionEntity();
        se.setId(userKey);
        se.setSource("feishu");
        se.setTitle("");
        se.setChatId(chatId);
        se.setChatType(chatType);
        se.setMode("knowledge");
        se.setCreatedAt(now);
        se.setUpdatedAt(now);
        sessionDbService.save(se);
        return se;
    }

    public void deleteSession(String sessionId) {
        messageDbService.getBaseMapper().delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MessageEntity>()
                        .eq("session_id", sessionId));
        sessionDbService.removeById(sessionId);
    }

    public void deleteMessagesByKb(Long kbId) {
        messageDbService.getBaseMapper().delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MessageEntity>()
                        .eq("kb_id", kbId));
    }

    // ========== Messages ==========

    public void saveMessage(String sessionId, String role, String content, String mode, String source) {
        saveMessage(sessionId, role, content, mode, source, null);
    }

    public void saveMessage(String sessionId, String role, String content, String mode, String source, Long kbId) {
        String now = TimeUtil.nowStr();

        MessageEntity msg = new MessageEntity();
        msg.setSessionId(sessionId);
        msg.setSource(source);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMode(mode != null ? mode : "knowledge");
        msg.setCreatedAt(now);
        msg.setKbId(kbId);
        messageDbService.save(msg);

        markSessionUpdated(sessionId, role, mode);
    }

    private void markSessionUpdated(String sessionId, String role, String mode) {
        SessionEntity se = sessionDbService.getById(sessionId);
        if (se == null) return;

        SessionEntity update = new SessionEntity();
        update.setId(sessionId);
        update.setUpdatedAt(TimeUtil.nowStr());
        if ("user".equals(role) && se.getTitle() != null && se.getTitle().equals("新对话")) {
            String title = contentPreview(sessionId);
            if (title != null) update.setTitle(title);
        }
        if (mode != null && !mode.isEmpty()) {
            update.setMode(mode);
        }
        sessionDbService.updateById(update);
    }

    private String contentPreview(String sessionId) {
        List<MessageEntity> msgs = messageDbService.listBySession(sessionId);
        for (MessageEntity m : msgs) {
            if ("user".equals(m.getRole())) {
                String t = m.getContent();
                return t.length() > 30 ? t.substring(0, 30) + "..." : t;
            }
        }
        return null;
    }

    // ========== History Context ==========

    public String buildHistoryContext(String sessionId, String mode) {
        List<MessageEntity> allMsgs = messageDbService.listBySession(sessionId);
        if (allMsgs.isEmpty()) return null;

        List<MessageEntity> filtered;
        if (mode != null && !mode.isEmpty()) {
            filtered = allMsgs.stream()
                    .filter(m -> mode.equals(m.getMode()))
                    .collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(allMsgs);
        }
        if (filtered.isEmpty()) return null;

        log.info("[ctx] 消息 {} 条", filtered.size());
        return buildSimpleContext(filtered);
    }

    private String buildSimpleContext(List<MessageEntity> filtered) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是之前的对话历史，供参考：\n\n");
        for (MessageEntity msg : filtered) {
            String label = "user".equals(msg.getRole()) ? "用户" : "AI";
            sb.append(label).append(": ").append(msg.getContent()).append("\n\n");
        }
        sb.append("---\n\n请基于以上历史对话，继续回复用户的最新消息。");
        return sb.toString();
    }

    // ========== Embedding utilities ==========
}
