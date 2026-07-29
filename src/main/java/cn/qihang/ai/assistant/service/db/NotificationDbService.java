package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.NotificationEntity;

import java.util.List;

public interface NotificationDbService extends IService<NotificationEntity> {
    List<NotificationEntity> listByUser(Long userId, int limit);
    List<NotificationEntity> listByUserAndType(Long userId, String type, int limit);
    int countUnread(Long userId);
    void markAllRead(Long userId);
    void addNotification(Long userId, String title, String content, String type, String sourceType, String sourceId);
}
