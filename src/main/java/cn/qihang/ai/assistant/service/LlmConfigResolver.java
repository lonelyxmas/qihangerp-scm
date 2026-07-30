package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import cn.qihang.ai.assistant.service.db.LlmProfileDbService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
@DependsOn("sessionService")
public class LlmConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(LlmConfigResolver.class);

    private final RestClient.Builder restClientBuilder;
    private final LlmProfileDbService llmProfileDbService;
    private final int defaultTimeoutSec;

    public LlmConfigResolver(RestClient.Builder restClientBuilder,
                             LlmProfileDbService llmProfileDbService,
                             @Value("${app.llm-timeout:600}") int defaultTimeoutSec) {
        this.restClientBuilder = restClientBuilder;
        this.llmProfileDbService = llmProfileDbService;
        this.defaultTimeoutSec = defaultTimeoutSec;
    }

    @PostConstruct
    public void init() {
        migrateLegacyConfig();
    }

    private void migrateLegacyConfig() {
        List<LlmProfileEntity> existing = llmProfileDbService.listAllOrdered();
        if (!existing.isEmpty()) return;

        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DEEPSEEK_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) return;

        String baseUrl = System.getenv("LLM_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) baseUrl = "https://api.deepseek.com";
        String model = System.getenv("LLM_MODEL");
        if (model == null || model.isEmpty()) model = "deepseek-chat";

        LlmProfileEntity profile = new LlmProfileEntity();
        profile.setName("default");
        profile.setApiKey(apiKey);
        profile.setBaseUrl(baseUrl);
        profile.setModel(model);
        profile.setTimeout(600);
        profile.setIsDefault(true);
        llmProfileDbService.save(profile);
        log.info("Migrated legacy LLM config to ai_llm_profiles table (model={})", model);
    }

    private LlmProfileEntity resolveDefaultProfile() {
        LlmProfileEntity profile = llmProfileDbService.findDefault();
        if (profile == null) {
            List<LlmProfileEntity> all = llmProfileDbService.listAllOrdered();
            if (!all.isEmpty()) {
                profile = all.get(0);
            }
        }
        return profile;
    }

    private LlmProfileEntity resolveProfile(String name) {
        if (name == null || name.isEmpty()) return resolveDefaultProfile();
        LlmProfileEntity profile = llmProfileDbService.findByName(name);
        if (profile == null) return resolveDefaultProfile();
        return profile;
    }

    public String resolveApiKey() {
        LlmProfileEntity p = resolveDefaultProfile();
        if (p != null) return p.getApiKey();
        String key = System.getenv("LLM_API_KEY");
        if (key != null && !key.isEmpty()) return key;
        key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isEmpty()) return key;
        return "";
    }

    public String resolveBaseUrl() {
        LlmProfileEntity p = resolveDefaultProfile();
        if (p != null) return p.getBaseUrl();
        String envUrl = System.getenv("LLM_BASE_URL");
        if (envUrl != null && !envUrl.isEmpty()) return envUrl;
        return "https://api.deepseek.com";
    }

    public String resolveModel() {
        LlmProfileEntity p = resolveDefaultProfile();
        if (p != null) return p.getModel();
        return "deepseek-chat";
    }

    public int resolveTimeout() {
        LlmProfileEntity p = resolveDefaultProfile();
        if (p != null && p.getTimeout() != null && p.getTimeout() > 0) return p.getTimeout();
        return defaultTimeoutSec;
    }

    public boolean isAvailable() {
        String apiKey = resolveApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    public RestClient.Builder buildRestClientBuilder() {
        int timeoutSec = resolveTimeout();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSec));
        return this.restClientBuilder.clone().requestFactory(requestFactory);
    }

    // ========== Profile-based resolution (for multi-model) ==========

    public String resolveApiKey(String profileName) {
        LlmProfileEntity p = resolveProfile(profileName);
        if (p != null) {
            String key = p.getApiKey();
            if (key != null && !key.isEmpty()) return key;
        }
        return resolveApiKey();
    }

    public String resolveBaseUrl(String profileName) {
        LlmProfileEntity p = resolveProfile(profileName);
        if (p != null) {
            String url = p.getBaseUrl();
            if (url != null && !url.isEmpty()) return url;
        }
        return resolveBaseUrl();
    }

    public String resolveModel(String profileName) {
        LlmProfileEntity p = resolveProfile(profileName);
        if (p != null) {
            String m = p.getModel();
            if (m != null && !m.isEmpty()) return m;
        }
        return resolveModel();
    }

    public int resolveTimeout(String profileName) {
        LlmProfileEntity p = resolveProfile(profileName);
        if (p != null && p.getTimeout() != null && p.getTimeout() > 0) return p.getTimeout();
        return resolveTimeout();
    }

    public List<LlmProfileEntity> getAllProfiles() {
        return llmProfileDbService.listAllOrdered();
    }

    public LlmProfileEntity getDefaultProfile() {
        return resolveDefaultProfile();
    }

    public LlmProfileEntity getProfileByName(String name) {
        return llmProfileDbService.findByName(name);
    }

    public LlmProfileEntity getEmbeddingProfile() {
        List<LlmProfileEntity> all = llmProfileDbService.listAllOrdered();
        for (LlmProfileEntity p : all) {
            if (LlmProfileEntity.TYPE_EMBEDDING.equals(p.getModelType())) {
                return p;
            }
        }
        return null;
    }
}
