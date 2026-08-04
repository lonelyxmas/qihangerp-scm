package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.LlmProfileEntity;
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

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final LlmConfigResolver llmConfigResolver;
    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;
    private EmbeddingModel embeddingModel;
    private boolean available;
    private String providerLabel = "";
    private String currentModelKey = "";

    public EmbeddingService(LlmConfigResolver llmConfigResolver,
                            RestClient.Builder restClientBuilder,
                            WebClient.Builder webClientBuilder) {
        this.llmConfigResolver = llmConfigResolver;
        this.restClientBuilder = restClientBuilder;
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        reloadConfig();
    }

    public void reloadConfig() {
        LlmProfileEntity profile = llmConfigResolver.getEmbeddingProfile();
        if (profile == null) {
            log.warn("⚠️ 未配置向量模型，请在配置页添加");
            this.available = false;
            return;
        }

        String model = profile.getModel();
        String baseUrl = profile.getBaseUrl();
        String apiKey = profile.getApiKey();
        String newKey = model + "|" + (apiKey != null ? apiKey : "");
        this.currentModelKey = newKey;
        this.providerLabel = extractProvider(baseUrl) + " · " + model;

        try {
            if (apiKey != null && !apiKey.isEmpty()) {
                initOpenAiEmbedding(model, baseUrl, apiKey);
            } else {
                initOllamaEmbedding(model, baseUrl);
            }
        } catch (Exception e) {
            log.warn("⚠️ Embedding 初始化失败: {}", e.getMessage());
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
        log.info("✅ 语义检索已就绪 (API模式: {}, model={})", provider, model);
    }

    private void initOllamaEmbedding(String model, String baseUrl) {
        try {
            java.net.http.HttpClient quickClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            JdkClientHttpRequestFactory quickFactory = new JdkClientHttpRequestFactory(quickClient);
            quickFactory.setReadTimeout(Duration.ofSeconds(3));
            RestClient checkClient = this.restClientBuilder.clone().requestFactory(quickFactory).build();

            String body = checkClient.get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .body(String.class);

            if (!body.contains("\"name\":\"" + model + "\"") && !body.contains("\"name\":\"" + model + ":")) {
                log.warn("⚠️ Embedding 模型 '{}' 未安装, 请执行: ollama pull {}", model, model);
                this.available = false;
                return;
            }

            // 检查 Ollama 版本是否支持 /api/embed (Ollama >= 0.3.0)
            try {
                String versionBody = checkClient.get()
                        .uri(baseUrl + "/api/version")
                        .retrieve()
                        .body(String.class);
                log.debug("Ollama version response: {}", versionBody);
            } catch (Exception ve) {
                log.warn("⚠️ Ollama 版本检测失败: {}", ve.getMessage());
            }
        } catch (Exception e) {
            log.warn("⚠️ Ollama 连接失败 ({}): {}, 语义检索不可用", baseUrl, e.getMessage());
            this.available = false;
            return;
        }

        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMinutes(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);
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
        log.info("✅ 语义检索已就绪 (Ollama模式: model={})", model);
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
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String resp = e.getResponseBodyAsString();
            log.warn("Embedding API 返回 {} ({}): {} — 请检查向量模型配置是否正确",
                    e.getStatusCode(), e.getStatusText(), resp != null ? resp : e.getMessage());
            log.warn("  常见原因: baseUrl 缺少 /v1 (如 https://api.siliconflow.cn → https://api.siliconflow.cn/v1)，或模型名不对，或 API Key 无效");
            return null;
        } catch (Exception e) {
            log.warn("Embedding 生成失败: {} — 请检查向量模型配置", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() { return available; }
    public String getProviderLabel() { return providerLabel; }
}
