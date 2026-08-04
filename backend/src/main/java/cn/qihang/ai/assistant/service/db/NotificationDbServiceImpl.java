package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.NotificationEntity;
import cn.qihang.ai.assistant.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationDbServiceImpl extends ServiceImpl<NotificationMapper, NotificationEntity> implements NotificationDbService {

    private final NotificationMapper notificationMapper;

    public NotificationDbServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<NotificationEntity> listByUser(Long userId, int limit) {
        return notificationMapper.listByUser(userId, limit);
    }

    @Override
    public List<NotificationEntity> listByUserAndType(Long userId, String type, int limit) {
        return notificationMapper.listByUserAndType(userId, type, limit);
    }

    @Override
    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAllRead(Long userId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        notificationMapper.markAllRead(userId, now);
    }

    @Override
    public void addNotification(Long userId, String title, String content, String type, String sourceType, String sourceId) {
        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(userId);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setType(type);
        notif.setSourceType(sourceType);
        notif.setSourceId(sourceId);
        notif.setIsRead(0);
        notif.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        save(notif);
    }
}
