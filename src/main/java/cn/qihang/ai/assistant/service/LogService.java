package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.ActivityLogEntity;
import cn.qihang.ai.assistant.model.LogEntry;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private static final int MAX_LOG = 100;

    private final ActivityLogDbService activityLogDbService;

    public LogService(ActivityLogDbService activityLogDbService) {
        this.activityLogDbService = activityLogDbService;
    }

    public List<LogEntry> load() {
        List<ActivityLogEntity> entities = activityLogDbService.lambdaQuery()
                .orderByDesc(ActivityLogEntity::getId)
                .last("LIMIT " + MAX_LOG)
                .list();
        return entities.stream().map(e -> {
            String raw = e.getActionType();
            String action = raw;
            String status = "";
            int sep = raw.lastIndexOf(" - ");
            if (sep > 0) {
                status = raw.substring(sep + 3);
                action = raw.substring(0, sep);
            }
            return new LogEntry(e.getCreatedAt(), action, status, e.getActionDesc());
        }).collect(Collectors.toList());
    }

    public synchronized void add(String action, String status, String detail) {
        ActivityLogEntity entity = new ActivityLogEntity();
        entity.setActionType(action + " - " + status);
        entity.setActionDesc(detail != null ? detail : "");
        entity.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        activityLogDbService.save(entity);
        long count = activityLogDbService.count();
        if (count > MAX_LOG) {
            List<ActivityLogEntity> oldest = activityLogDbService.lambdaQuery()
                    .orderByAsc(ActivityLogEntity::getId)
                    .last("LIMIT " + (count - MAX_LOG))
                    .list();
            if (!oldest.isEmpty()) {
                activityLogDbService.removeByIds(oldest.stream().map(ActivityLogEntity::getId).collect(Collectors.toList()));
            }
        }
    }

    public void add(String action, String status) {
        add(action, status, "");
    }
}
