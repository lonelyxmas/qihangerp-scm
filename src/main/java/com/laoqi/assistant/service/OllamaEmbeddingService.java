package com.laoqi.assistant.service;

import com.laoqi.assistant.config.AppConfig;
import com.laoqi.assistant.model.Config;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 向量化服务。支持两种模式：
 *   - Ollama 本地 (无 apiKey)
 *   - OpenAI 兼容 API (含 apiKey，如硅基流动)
 * 配置通过 config.json 的 embeddingModel / embeddingBaseUrl / embeddingApiKey 管理。
 */
@Service
public class OllamaEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final AppConfig appConfig;
    private final LogService logService;
    private final ConfigService configService;
    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;
    private EmbeddingModel embeddingModel;
    private boolean available;
    private String providerLabel = "";
    private String currentModelKey = "";  // 用于检测模型是否变更
    public OllamaEmbeddingService(AppConfig appConfig, LogService logService,
                                   ConfigService configService,
                                   RestClient.Builder restClientBuilder,
                                   WebClient.Builder webClientBuilder) {
        this.appConfig = appConfig;
        this.logService = logService;
        this.configService = configService;
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        reloadConfig();
    }

    public void reloadConfig() {
        Config cfg = configService.load();
        String model = cfg.getEmbeddingModel();
        String baseUrl = cfg.getEmbeddingBaseUrl();
        String apiKey = cfg.getEmbeddingApiKey();
        String providerConfig = cfg.getEmbeddingProvider();

        if (model == null || model.isEmpty()) model = appConfig.getOllamaModel();
        if (baseUrl == null || baseUrl.isEmpty()) baseUrl = appConfig.getOllamaBaseUrl();

        // 仅记录当前模型 key，实际清空由 ApiConfigController 在保存前处理
        String newKey = model + "|" + apiKey;
        this.currentModelKey = newKey;

        this.providerLabel = providerConfig != null && !providerConfig.isEmpty()
                ? providerConfig + " · " + model
                : "";

        try {
            if (apiKey != null && !apiKey.isEmpty()) {
                initOpenAiEmbedding(model, baseUrl, apiKey);
            } else {
                initOllamaEmbedding(model, baseUrl);
            }
        } catch (Exception e) {
            log.warn("⚠️ Embedding 初始化失败: {}", e.getMessage());
            logService.add("Embedding", "不可用", e.getMessage());
            this.available = false;
        }
    }

    private void initOpenAiEmbedding(String model, String baseUrl, String apiKey) {
        String provider = extractProvider(baseUrl);

        com.openai.core.http.HttpClient httpClient = org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient.builder()
                .build();
        com.openai.core.ClientOptions options = com.openai.core.ClientOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .httpClient(httpClient)
                .build();
        com.openai.client.OpenAIClient client = new com.openai.client.OpenAIClientImpl(options);

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .openAiClient(client)
                .options(OpenAiEmbeddingOptions.builder()
                        .model(model)
                        .build())
                .build();

        this.available = true;
        if (this.providerLabel.isEmpty()) {
            this.providerLabel = provider + " · " + model;
        }
        log.info("✅ 语义检索已就绪 (API模式: {}, model={})", provider, model);
        logService.add("Embedding", "就绪", provider + " / " + model);
    }

    private void initOllamaEmbedding(String model, String baseUrl) {
        // 快速健康检查：Ollama 是否运行 & 模型是否安装
        try {
            java.net.http.HttpClient quickClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            org.springframework.http.client.JdkClientHttpRequestFactory quickFactory =
                    new org.springframework.http.client.JdkClientHttpRequestFactory(quickClient);
            quickFactory.setReadTimeout(Duration.ofSeconds(3));
            RestClient checkClient = this.restClientBuilder.clone().requestFactory(quickFactory).build();

            String body = checkClient.get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .body(String.class);

            if (!body.contains("\"name\":\"" + model + "\"") && !body.contains("\"name\":\"" + model + ":")) {
                log.warn("⚠️ Embedding 模型 '{}' 未安装, 请执行: ollama pull {}", model, model);
                logService.add("Ollama", "模型未安装", "请执行: ollama pull " + model);
                this.available = false;
                return;
            }
        } catch (Exception e) {
            log.warn("⚠️ Ollama 连接失败 ({}): {}, 语义检索不可用", baseUrl, e.getMessage());
            logService.add("Ollama", "不可用", e.getMessage());
            this.available = false;
            return;
        }

        // 基于 Spring Boot 已自动配置的 RestClient/WebClient Builder 增加超时
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMinutes(10))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(jdkHttpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(10));

        RestClient.Builder rcBuilder = this.restClientBuilder.clone()
                .requestFactory(requestFactory);

        reactor.netty.http.client.HttpClient nettyHttpClient =
                reactor.netty.http.client.HttpClient.create()
                        .responseTimeout(Duration.ofMinutes(10));
        org.springframework.http.client.reactive.ReactorClientHttpConnector connector =
                new org.springframework.http.client.reactive.ReactorClientHttpConnector(nettyHttpClient);

        WebClient.Builder wcBuilder = this.webClientBuilder.clone()
                .clientConnector(connector);

        this.embeddingModel = org.springframework.ai.ollama.OllamaEmbeddingModel.builder()
                .ollamaApi(org.springframework.ai.ollama.api.OllamaApi.builder()
                        .baseUrl(baseUrl)
                        .restClientBuilder(rcBuilder)
                        .webClientBuilder(wcBuilder)
                        .build())
                .options(org.springframework.ai.ollama.api.OllamaEmbeddingOptions.builder()
                        .model(model)
                        .build())
                .build();

        this.available = true;
        if (this.providerLabel.isEmpty()) {
            this.providerLabel = "Ollama · " + model;
        }
        log.info("✅ 语义检索已就绪 (Ollama模式: model={})", model);
        logService.add("Ollama", "就绪", "model=" + model);
    }

    private String extractProvider(String baseUrl) {
        if (baseUrl == null) return "API";
        String lower = baseUrl.toLowerCase();
        if (lower.contains("siliconflow")) return "硅基流动";
        if (lower.contains("openai")) return "OpenAI";
        if (lower.contains("deepseek")) return "DeepSeek";
        if (lower.contains("dashscope") || lower.contains("aliyun")) return "阿里云百炼";
        if (lower.contains("sensenova")) return "商汤日日新";
        return "API";
    }

    public float[] embed(String text) {
        if (!available || embeddingModel == null) return null;
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.warn("Embedding 生成失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() { return available; }
    public String getProviderLabel() { return providerLabel; }
}
