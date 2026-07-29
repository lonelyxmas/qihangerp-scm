package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.datacenter.model.DataSet;
import cn.qihang.ai.assistant.entity.DataSetRecordEntity;
import cn.qihang.ai.assistant.model.CollabConfig;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import cn.qihang.ai.assistant.service.db.DataSetRecordDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CollabEngine {

    private static final Logger log = LoggerFactory.getLogger(CollabEngine.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityLogDbService activityLogDbService;
    private final NotificationDbService notificationDbService;
    private final DataSetRecordDbService recordDbService;
    private final FeishuService feishuService;

    public CollabEngine(ActivityLogDbService activityLogDbService,
                        NotificationDbService notificationDbService,
                        DataSetRecordDbService recordDbService,
                        FeishuService feishuService) {
        this.activityLogDbService = activityLogDbService;
        this.notificationDbService = notificationDbService;
        this.recordDbService = recordDbService;
        this.feishuService = feishuService;
    }

    public void processNewRecord(DataSet ds, DataSetRecordEntity entity) {
        if (ds == null || ds.getCollabConfig() == null) return;
        CollabConfig cfg = ds.getCollabConfig();
        String now = LocalDateTime.now().format(FMT);
        boolean needsUpdate = false;

        if (cfg.getAssignedToField() != null && !cfg.getAssignedToField().isEmpty()) {
            entity.setAssignedAt(now);
            needsUpdate = true;

            if (Boolean.TRUE.equals(cfg.getNotifyAssigneeOnCreate())) {
                notificationDbService.addNotification(
                        1L,
                        "新记录指派",
                        "您被指派为「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 的负责人",
                        "task_assignment", "dataset", entity.getRecordId()
                );
                activityLogDbService.addLog("assign_task",
                        "自动指派记录 #" + entity.getRecordNum() + " 到「" + ds.getName() + "」",
                        "system", null, "系统");
            }
        }

        if (needsUpdate) {
            recordDbService.updateById(entity);
        }

        sendFeishuNotification(cfg, "新增记录", "「" + ds.getName() + "」新增记录 #" + entity.getRecordNum());
    }

    public void processUpdatedRecord(DataSet ds, DataSetRecordEntity entity) {
        if (ds == null || ds.getCollabConfig() == null) return;
        CollabConfig cfg = ds.getCollabConfig();
        String now = LocalDateTime.now().format(FMT);
        boolean needsUpdate = false;

        if (cfg.getAssignedToField() != null && !cfg.getAssignedToField().isEmpty()) {
            entity.setAssignedAt(now);
            needsUpdate = true;
        }

        if (needsUpdate) {
            recordDbService.updateById(entity);
        }

        if (Boolean.TRUE.equals(cfg.getNotifyOnUpdate())) {
            notificationDbService.addNotification(
                    1L,
                    "记录更新",
                    "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已更新",
                    "record_update", "dataset", entity.getRecordId()
            );
            activityLogDbService.addLog("record_update",
                    "记录 #" + entity.getRecordNum() + " 在「" + ds.getName() + "」已更新",
                    "system", null, "系统");
        }

        sendFeishuNotification(cfg, "记录更新", "「" + ds.getName() + "」记录 #" + entity.getRecordNum() + " 已更新");
    }

    private void sendFeishuNotification(CollabConfig cfg, String title, String content) {
        if (cfg.getFeishuWebhookUrl() != null && !cfg.getFeishuWebhookUrl().isEmpty()) {
            feishuService.sendPost("【数据协作】" + title, java.util.List.of(
                    java.util.List.of(java.util.Map.of("tag", "text", "text", content))
            ));
        }
    }
}
