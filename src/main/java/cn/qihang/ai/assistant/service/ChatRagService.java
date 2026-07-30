package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Chat RAG 服务 — 纯文本流式对话，无工具调用。
 * 通过知识库检索（RAG）获取上下文，配合对话历史，
 * 使用 Spring AI 原生 .stream().content() 实现真流式 SSE。
 */
@Service
public class ChatRagService {

    private static final Logger log = LoggerFactory.getLogger(ChatRagService.class);

    /** 系统 Prompt：基于知识库内容回答，引用来源，未知时如实告知 */
    private static final String SYSTEM_PROMPT = """
            你是一个 AI 助手，基于知识库内容回答用户的问题。
            请根据提供的知识库内容回答。如果内容不足以回答，请如实告知用户。
            回答时引用信息来源的文件名。使用中文回复。
            """;

    private final LlmConfigResolver configResolver;
    private final NoteIndexService noteIndexService;
    private final SessionService sessionService;

    private volatile ChatClient defaultClient;
    private volatile String cachedConfigKey = "";
    private final ConcurrentHashMap<String, ChatClient> modelClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> modelConfigKeys = new ConcurrentHashMap<>();

    public ChatRagService(LlmConfigResolver configResolver,
                          NoteIndexService noteIndexService,
                          SessionService sessionService) {
        this.configResolver = configResolver;
        this.noteIndexService = noteIndexService;
        this.sessionService = sessionService;
    }

    public boolean isAvailable() {
        return configResolver.isAvailable();
    }

    /**
     * 流式 RAG Chat：检索 KB 上下文 + 对话历史 → 真流式输出。
     *
     * @param sessionId      会话 ID
     * @param userMessage    用户消息
     * @param kbId           知识库 ID（可为空，跳过检索）
     * @param modelName      模型名称（可为空，使用默认模型）
     * @param chunkCallback  每个流式 chunk 的回调
     * @param statusCallback 状态变更回调
     * @return 完整回复文本
     */
    public String streamChat(String sessionId,
                             String userMessage,
                             Long kbId,
                             String modelName,
                             Consumer<String> chunkCallback,
                             Consumer<String> statusCallback) throws Exception {

        ChatClient client = getOrCreateClient(modelName);
        if (client == null) {
            throw new IllegalStateException("LLM API Key 未配置，请在配置页填写");
        }

        StringBuilder fullReply = new StringBuilder();

        try {
            if (statusCallback != null) {
                statusCallback.accept("正在检索知识库...");
            }

            // Step 1: 构建 RAG 上下文（KB 检索 + 对话历史）
            String ragContext = buildContext(sessionId, userMessage, kbId);

            if (statusCallback != null) {
                statusCallback.accept("AI 正在生成回复...");
            }

            // Step 2: 真流式调用
            String finalRagContext = ragContext;
            try {
                client.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(finalRagContext + "\n\n用户: " + userMessage)
                        .stream()
                        .content()
                         .doOnNext(chunk -> {
                             if (chunk != null && !chunk.isBlank()) {
                                 fullReply.append(chunk);
                                if (chunkCallback != null) {
                                    chunkCallback.accept(chunk);
                                }
                            }
                        })
                        .blockLast();
            } catch (Exception e) {
                if (statusCallback != null) {
                    statusCallback.accept("AI 生成失败：" + e.getMessage());
                }
                log.warn("[ChatRAG] 流式调用失败", e);
                throw e;
            }

            if (fullReply.length() == 0) {
                fullReply.append("（AI 未返回回复）");
                if (chunkCallback != null) {
                    chunkCallback.accept("（AI 未返回回复）");
                }
            }

            log.info("[ChatRAG] 流式回复完成, 总长度={}, kbId={}, model={}",
                    fullReply.length(), kbId, modelName != null ? modelName : "default");

            return fullReply.toString();

        } finally {
            // 清理缓存引用
        }
    }

    /**
     * 构建 RAG 上下文：
     * 1. 检索知识库相关片段
     * 2. 拼接最近对话历史
     * 3. 组合成统一的上下文字符串
     */
    private String buildContext(String sessionId, String userMessage, Long kbId) {
        StringBuilder sb = new StringBuilder();

        // 1. 知识库检索
        if (kbId != null) {
            try {
                if (noteIndexService.isAvailable()) {
                    List<NoteIndexService.NoteSearchResult> results =
                            noteIndexService.hybridSearch(kbId, userMessage, 5);

                    if (!results.isEmpty()) {
                        sb.append("== 知识库检索结果 ==\n\n");
                        for (NoteIndexService.NoteSearchResult r : results) {
                            String source = r.filePath() != null ? r.filePath() : "未知文件";
                            String content = r.content() != null ? r.content() : "";
                            float score = r.score();
                            sb.append("【来源: ").append(source).append("】(匹配度: ").append(score).append(")\n");
                            sb.append(content).append("\n\n");
                        }
                    } else {
                        sb.append("== 知识库检索结果 ==\n\n未找到相关知识库内容。\n\n");
                    }
                } else {
                    sb.append("== 知识库检索结果 ==\n\nEmbedding 服务不可用，无法检索知识库。\n\n");
                }
            } catch (Exception e) {
                log.warn("[ChatRAG] KB 检索失败: {}", e.getMessage());
                sb.append("== 知识库检索结果 ==\n\n检索失败: ").append(e.getMessage()).append("\n\n");
            }
        } else {
            sb.append("== 知识库检索结果 ==\n\n未指定知识库，跳过检索。\n\n");
        }

        // 2. 对话历史（最近 20 条）
        String history = sessionService.buildSimpleHistory(sessionId);
        if (history != null && !history.isBlank()) {
            sb.append("== 对话历史 ==\n\n").append(history).append("\n\n");
        }

        return sb.toString();
    }

    // ========== ChatClient 管理 ==========

    private ChatClient getOrCreateClient(String modelName) {
        if (!isAvailable()) return null;

        if (modelName == null || modelName.isEmpty()) {
            String currentKey = buildConfigKey();
            if (defaultClient == null || !currentKey.equals(cachedConfigKey)) {
                defaultClient = createDefaultClient();
                cachedConfigKey = currentKey;
                log.info("ChatRagService ChatClient 已重建 (default)");
            }
            return defaultClient;
        }

        String cacheKey = modelName;
        String currentKey = buildConfigKey(modelName);
        String cachedKey = modelConfigKeys.get(cacheKey);
        if (cachedKey == null || !cachedKey.equals(currentKey)) {
            LlmProfileEntity profile = configResolver.getProfileByName(modelName);
            if (profile != null) {
                ChatClient client = createClient(profile);
                modelClients.put(cacheKey, client);
                modelConfigKeys.put(cacheKey, currentKey);
                log.info("ChatRagService ChatClient 已重建 (model={})", modelName);
            } else {
                return defaultClient;
            }
        }
        return modelClients.get(cacheKey);
    }

    private String buildConfigKey() {
        return configResolver.resolveBaseUrl() + "|"
                + configResolver.resolveModel() + "|"
                + configResolver.resolveApiKey().hashCode();
    }

    private String buildConfigKey(String modelName) {
        return configResolver.resolveBaseUrl(modelName) + "|"
                + configResolver.resolveModel(modelName) + "|"
                + configResolver.resolveApiKey(modelName).hashCode();
    }

    private ChatClient createDefaultClient() {
        String apiKey = configResolver.resolveApiKey();
        String baseUrl = configResolver.resolveBaseUrl();
        String model = configResolver.resolveModel();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(configResolver.buildRestClientBuilder())
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(model)
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .options(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    private ChatClient createClient(LlmProfileEntity profile) {
        if (profile.isMultimodal()) {
            return buildOpenAiClient(profile);
        }

        String apiKey = profile.getApiKey();
        String baseUrl = profile.getBaseUrl();
        String model = profile.getModel();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(configResolver.buildRestClientBuilder())
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(model)
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .options(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    private ChatClient buildOpenAiClient(LlmProfileEntity profile) {
        String apiKey = profile.getApiKey();
        String baseUrl = profile.getBaseUrl();
        String model = profile.getModel();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        int timeoutSec = (profile.getTimeout() != null && profile.getTimeout() > 0)
                ? profile.getTimeout() : 600;
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSec))
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(options)
                .observationRegistry(io.micrometer.observation.ObservationRegistry.NOOP)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
