package cn.qihang.ai.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.qihang.ai.assistant.config.AppConfig;
import cn.qihang.ai.assistant.model.Config;
import cn.qihang.ai.assistant.util.FileUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final AppConfig appConfig;

    public ConfigService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    private void ensureConfigFile() {
        Path configFile = appConfig.getConfigFile();
        if (Files.exists(configFile)) return;
        Path template = appConfig.getConfigDirPath().resolve("config.template.json");
        if (Files.exists(template)) {
            try {
                Files.copy(template, configFile);
                log.info("config.json 不存在，已从 config.template.json 创建");
            } catch (Exception e) {
                log.warn("无法从模板创建 config.json: {}", e.getMessage());
            }
        }
    }

    public Config load() {
        ensureConfigFile();

        Map<String, Object> raw = FileUtil.readJson(appConfig.getConfigFile(), MAP_TYPE, null);

        Config config;
        if (raw != null) {
            config = new ObjectMapper().convertValue(raw, Config.class);
        } else {
            config = Config.defaultConfig("");
        }
        mergeDefaultValues(config);
        return config;
    }

    private void mergeDefaultValues(Config config) {
        Config defaultConfig = Config.defaultConfig("");
        
        if (config.isFeishuPollingEnabled() == null) {
            config.setFeishuPollingEnabled(defaultConfig.isFeishuPollingEnabled());
        }
    }

    public void save(Config config) {
        mergeDefaultValues(config);
        FileUtil.writeJson(appConfig.getConfigFile(), config);
    }
}
