package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.datacenter.model.DataSet;
import cn.qihang.ai.assistant.entity.DataSetRecordEntity;
import cn.qihang.ai.assistant.model.CollabConfig;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CollabEngine {

    private static final Logger log = LoggerFactory.getLogger(CollabEngine.class);

    private final ActivityLogDbService activityLogDbService;
    private final NotificationDbService notificationDbService;
    private final FeishuService feishuService;
    private final FeishuConfigResolver feishuConfigResolver;

    public CollabEngine(ActivityLogDbService activityLogDbService,
                        NotificationDbService notificationDbService,
                        FeishuService feishuService,
                        FeishuConfigResolver feishuConfigResolver) {
        this.activityLogDbService = activityLogDbService;
        this.notificationDbService = notificationDbService;
        this.feishuService = feishuService;
        this.feishuConfigResolver = feishuConfigResolver;
    }

    public void processNewRecord(DataSet ds, DataSetRecordEntity entity) {
        if (ds == null || ds.getCollabConfig() == null) return;
        CollabConfig cfg = ds.getCollabConfig();

        notificationDbService.addNotification(
                1L,
                "新增记录",
                "「" + ds.getName() + "」新增记录 #" + entity.getRecordNum(),
                "record_create", "dataset", entity.getRecordId()
        );
        activityLogDbService.addLog("record_create",
                "记录 #" + entity.getRecordNum() + " 在「" + ds.getName() + "」已创建",
                "system", null, "系统");

        if (Boolean.TRUE.equals(cfg.getFeishuNotify())) {
            sendFeishu("新增记录", "「" + ds.getName() + "」新增记录 #" + entity.getRecordNum());
        }
    }

    public void processUpdatedRecord(DataSet ds, DataSetRecordEntity entity) {
        if (ds == null || ds.getCollabConfig() == null) return;
        CollabConfig cfg = ds.getCollabConfig();

        notificationDbService.addNotification(
                1L,
                "记录更新",
                "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已更新",
                "record_update", "dataset", entity.getRecordId()
        );
        activityLogDbService.addLog("record_update",
                "记录 #" + entity.getRecordNum() + " 在「" + ds.getName() + "」已更新",
                "system", null, "系统");

        if (Boolean.TRUE.equals(cfg.getFeishuNotify())) {
            sendFeishu("记录更新", "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已更新");
        }
    }

    public void processDeletedRecord(DataSet ds, DataSetRecordEntity entity) {
        if (ds == null || ds.getCollabConfig() == null) return;
        CollabConfig cfg = ds.getCollabConfig();

        notificationDbService.addNotification(
                1L,
                "记录删除",
                "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已删除",
                "record_delete", "dataset", entity.getRecordId()
        );
        activityLogDbService.addLog("record_delete",
                "记录 #" + entity.getRecordNum() + " 在「" + ds.getName() + "」已删除",
                "system", null, "系统");

        if (Boolean.TRUE.equals(cfg.getFeishuNotify())) {
            sendFeishu("记录删除", "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已删除");
        }
    }

    private void sendFeishu(String title, String content) {
        String webhookUrl = feishuConfigResolver.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        feishuService.sendPost("【数据协作】" + title, List.of(
                List.of(Map.of("tag", "text", "text", content))
        ));
    }
}