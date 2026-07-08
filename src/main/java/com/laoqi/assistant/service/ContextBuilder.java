package com.laoqi.assistant.service;

import com.laoqi.assistant.entity.KnowledgeBaseEntity;
import com.laoqi.assistant.entity.MessageEntity;
import com.laoqi.assistant.util.FileUtil;
import com.laoqi.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Service
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    private final SessionService sessionService;
    private final KnowledgeBaseService kbService;

    public ContextBuilder(SessionService sessionService,
                          KnowledgeBaseService kbService) {
        this.sessionService = sessionService;
        this.kbService = kbService;
    }

    /**
     * 构建完整上下文
     * 1. 注入历史对话
     * 2. 读取规则文件
     */
    public ChatContext build(String sessionId, String userMessage, Long kbId) {
        return build(sessionId, userMessage, kbId, null);
    }

    public ChatContext build(String sessionId, String userMessage, Long kbId, Consumer<String> statusCallback) {
        if (statusCallback != null) statusCallback.accept("正在加载历史对话...");
        String historyContext = sessionService.buildHistoryContext(sessionId, "knowledge", userMessage);

        String notesDir = null;
        if (kbId != null) {
            notesDir = kbService.getNotesDirById(kbId);
        }

        if (statusCallback != null) statusCallback.accept("正在读取规则文件...");
        String agentsMd = "";
        if (notesDir != null && !notesDir.isBlank()) {
            try {
                Path agentsFile = Paths.get(notesDir, "AGENTS.md");
                if (agentsFile.toFile().exists()) {
                    agentsMd = FileUtil.readText(agentsFile);
                }
            } catch (Exception e) {
                log.warn("[ContextBuilder] 读取 AGENTS.md 失败: {}", e.getMessage());
            }
        }

        if (kbId == null) {
            if (statusCallback != null) statusCallback.accept("未指定知识库，请使用 @笔记库名 指定");
        } else {
            if (statusCallback != null) statusCallback.accept("上下文构建完成，正在请求 AI...");
        }
        return new ChatContext(historyContext, notesDir, agentsMd);
    }

    /**
     * 将上下文合并为完整的消息
     */
    public String merge(ChatContext context, String userMessage) {
        StringBuilder sb = new StringBuilder();

        String dateStr = TimeUtil.todayStr();
        String weekday = TimeUtil.weekdayCn(TimeUtil.now());
        sb.append("== 当前时间 ==\n");
        sb.append("日期: ").append(dateStr).append(" (").append(weekday).append(")\n");
        sb.append("请以当前日期为基准理解'今天'、'本周'、'本月'等时间概念。\n\n");

        boolean hasMention = userMessage != null && userMessage.contains("@");
        if (!hasMention) {
            sb.append("== 提示 ==\n");
            sb.append("用户的问题中没有指定笔记库（使用 @笔记库名 格式）。\n");
            sb.append("如果用户的问题需要搜索笔记内容，请提示用户使用 @笔记库名 来指定要搜索的笔记库，例如：'@工作笔记 查一下项目进展'。\n");
            sb.append("如果是一般性问题，可以直接回答，不需要搜索笔记库。\n\n");
        }

        if (context.agentsMd() != null && !context.agentsMd().isBlank()) {
            sb.append("== 规则文件 (AGENTS.md) ==\n");
            sb.append(context.agentsMd()).append("\n\n");
        }

        if (context.notesDir() != null && !context.notesDir().isBlank()) {
            sb.append("== 笔记库路径 ==\n");
            sb.append("用户指定的笔记库路径为: ").append(context.notesDir()).append("\n");
            sb.append("你可以使用 listDir、readFile、searchFiles 等工具浏览和读取该目录下的笔记文件。\n\n");
        }

        if (context.historyContext() != null && !context.historyContext().isBlank()) {
            sb.append(context.historyContext()).append("\n\n");
        }

        sb.append("---\n\n用户最新消息:\n").append(userMessage);

        return sb.toString();
    }

    /**
     * 上下文数据模型
     */
    public record ChatContext(
            String historyContext,
            String notesDir,
            String agentsMd
    ) {}
}
