package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicChatController {

    private static final Logger log = LoggerFactory.getLogger(PublicChatController.class);

    private static final String SANDBOX_SYSTEM_PROMPT = """
            你是一个公开演示用的 AI 助手（沙箱模式）。
            重要规则：
            1. 你没有任何用户的业务数据、知识库或上下文信息。
            2. 你只能回答通用知识、常识性问题，或进行日常对话。
            3. 如果用户问及具体的业务数据、客户信息、项目细节等，请回答："我是公开沙箱模式，无法访问任何业务数据。请登录后绑定知识库获取完整能力。"
            4. 保持友好、热情的语气。
            5. 回答简洁明了。
            """;

    private final LlmService llmService;

    public PublicChatController(LlmService llmService) {
        this.llmService = llmService;
    }

    @PostMapping("/chat/send")
    public ResponseEntity<Map<String, Object>> publicChat(@RequestParam String message) {
        if (message == null || message.isBlank()) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "消息不能为空"));
        }
        try {
            if (!llmService.isAvailable()) {
                return ResponseEntity.ok(Map.of("ok", false, "error", "LLM API Key 未配置"));
            }
            String reply = llmService.chat(SANDBOX_SYSTEM_PROMPT, message);
            return ResponseEntity.ok(Map.of("ok", true, "reply", reply));
        } catch (Exception e) {
            log.error("公开沙箱对话失败", e);
            return ResponseEntity.ok(Map.of("ok", false, "error", "处理失败: " + e.getMessage()));
        }
    }
}
