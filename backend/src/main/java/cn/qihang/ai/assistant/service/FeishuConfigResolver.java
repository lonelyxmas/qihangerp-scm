package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.service.db.AppConfigDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeishuConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(FeishuConfigResolver.class);

    private static final String KEY_WEBHOOK_URL = "feishu.webhookUrl";
    private static final String KEY_APP_ID = "feishu.appId";
    private static final String KEY_APP_SECRET = "feishu.appSecret";
    private static final String KEY_CHAT_ID = "feishu.chatId";


    private final AppConfigDbService appConfigDbService;

    public FeishuConfigResolver(AppConfigDbService appConfigDbService) {
        this.appConfigDbService = appConfigDbService;
    }

    public String getWebhookUrl() {
        return appConfigDbService.findValueByKey(KEY_WEBHOOK_URL);
    }

    public String getAppId() {
        return appConfigDbService.findValueByKey(KEY_APP_ID);
    }

    public String getAppSecret() {
        return appConfigDbService.findValueByKey(KEY_APP_SECRET);
    }

    public String getChatId() {
        return appConfigDbService.findValueByKey(KEY_CHAT_ID);
    }

    public void saveWebhookUrl(String webhookUrl) {
        appConfigDbService.saveOrUpdateByKey(KEY_WEBHOOK_URL, webhookUrl);
        log.info("[飞书配置] Webhook URL 已更新");
    }

    public void saveFeishuConfig(String appId, String appSecret, String chatId) {
        appConfigDbService.saveOrUpdateByKey(KEY_APP_ID, appId != null ? appId : "");
        appConfigDbService.saveOrUpdateByKey(KEY_APP_SECRET, appSecret != null ? appSecret : "");
        appConfigDbService.saveOrUpdateByKey(KEY_CHAT_ID, chatId != null ? chatId : "");
        log.info("[飞书配置] 消息接收配置已更新");
    }

    public void saveAll(String webhookUrl, String appId, String appSecret, String chatId) {
        saveWebhookUrl(webhookUrl);
        saveFeishuConfig(appId, appSecret, chatId);
    }
}
