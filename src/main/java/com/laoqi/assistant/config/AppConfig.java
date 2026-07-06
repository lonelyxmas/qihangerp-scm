package com.laoqi.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String configDir;
    private String timezone = "Asia/Shanghai";
    private int maxHistoryChars = 6000;

    public Path getConfigDirPath() {
        String dir = configDir;
        if (dir == null || dir.isEmpty()) {
            dir = System.getProperty("user.dir", ".");
        }
        // 如果配置了 %XXX% 格式的环境变量，直接解析
        if (dir.contains("%")) {
            dir = resolveEnvVars(dir);
        }
        return Paths.get(dir);
    }

    /** 解析字符串中的 %VAR% 环境变量 */
    private static String resolveEnvVars(String s) {
        int start = s.indexOf('%');
        if (start < 0) return s;
        int end = s.indexOf('%', start + 1);
        if (end < 0) return s;
        String varName = s.substring(start + 1, end);
        String value = System.getenv(varName);
        if (value == null) value = "";
        return resolveEnvVars(s.substring(0, start) + value + s.substring(end + 1));
    }

    public Path getConfigFile() {
        return getConfigDirPath().resolve("config.json");
    }

    public String getConfigDir() { return configDir; }
    public void setConfigDir(String configDir) { this.configDir = configDir; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getMaxHistoryChars() { return maxHistoryChars; }
    public void setMaxHistoryChars(int maxHistoryChars) { this.maxHistoryChars = maxHistoryChars; }

    // Ollama embedding config
    private String ollamaBaseUrl = "http://127.0.0.1:11434";
    private String ollamaModel = "bge-m3";
    private int ollamaTimeoutSeconds = 30;

    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }
    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }
    public int getOllamaTimeoutSeconds() { return ollamaTimeoutSeconds; }
    public void setOllamaTimeoutSeconds(int ollamaTimeoutSeconds) { this.ollamaTimeoutSeconds = ollamaTimeoutSeconds; }
}
