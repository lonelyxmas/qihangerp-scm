package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.MessageEntity;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    private final SessionService sessionService;

    public ContextBuilder(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public ChatContext build(String sessionId, String userMessage, Long kbId) {
        return build(sessionId, userMessage, kbId, null);
    }

    public ChatContext build(String sessionId, String userMessage, Long kbId, Consumer<String> statusCallback) {
        if (statusCallback != null) statusCallback.accept("正在加载历史对话...");
        String historyContext = sessionService.buildHistoryContext(sessionId, "knowledge");

        if (kbId == null) {
            if (statusCallback != null) statusCallback.accept("未指定知识库，请使用 @笔记库名 指定");
        } else {
            if (statusCallback != null) statusCallback.accept("上下文构建完成，正在请求 AI...");
        }
        return new ChatContext(historyContext, "");
    }

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
            sb.append("如果用户的问题需要搜索笔记内容，请提示用户使用 @笔记库名 来指定要搜索的笔记库。\n");
            sb.append("如果是一般性问题，可以直接回答，不需要搜索笔记库。\n\n");
        }

        if (context.agentsMd() != null && !context.agentsMd().isBlank()) {
            sb.append("== 规则文件 (AGENTS.md) ==\n");
            sb.append(context.agentsMd()).append("\n\n");
        }

        if (context.historyContext() != null && !context.historyContext().isBlank()) {
            sb.append(context.historyContext()).append("\n\n");
        }

        sb.append("---\n\n用户最新消息:\n").append(userMessage);

        return sb.toString();
    }

    public record ChatContext(
            String historyContext,
            String agentsMd
    ) {}
}