package cn.qihang.ai.assistant.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Config {

    private String feishuWebhookUrl;
    private String feishuAppId;
    private String feishuAppSecret;
    private String feishuChatId;

    public Config() {}

    public String getFeishuWebhookUrl() { return feishuWebhookUrl; }
    public void setFeishuWebhookUrl(String feishuWebhookUrl) { this.feishuWebhookUrl = feishuWebhookUrl; }
    public String getFeishuAppId() { return feishuAppId; }
    public void setFeishuAppId(String feishuAppId) { this.feishuAppId = feishuAppId; }
    public String getFeishuAppSecret() { return feishuAppSecret; }
    public void setFeishuAppSecret(String feishuAppSecret) { this.feishuAppSecret = feishuAppSecret; }
    public String getFeishuChatId() { return feishuChatId; }
    public void setFeishuChatId(String feishuChatId) { this.feishuChatId = feishuChatId; }

}
