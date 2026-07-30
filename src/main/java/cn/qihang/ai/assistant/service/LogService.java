package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.SystemLogEntity;
import cn.qihang.ai.assistant.model.LogEntry;
import cn.qihang.ai.assistant.service.db.SystemLogDbService;
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

    private final SystemLogDbService systemLogDbService;

    public LogService(SystemLogDbService systemLogDbService) {
        this.systemLogDbService = systemLogDbService;
    }

    public List<LogEntry> load() {
        List<SystemLogEntity> entities = systemLogDbService.lambdaQuery()
                .orderByDesc(SystemLogEntity::getId)
                .last("LIMIT " + MAX_LOG)
                .list();
        return entities.stream().map(e ->
                new LogEntry(e.getCreatedAt(), e.getAction(), e.getStatus(), e.getDetail())
        ).collect(Collectors.toList());
    }

    public synchronized void add(String action, String status, String detail) {
        SystemLogEntity entity = new SystemLogEntity();
        entity.setAction(action);
        entity.setStatus(status);
        entity.setDetail(detail != null ? detail : "");
        entity.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        systemLogDbService.save(entity);
        long count = systemLogDbService.count();
        if (count > MAX_LOG) {
            List<SystemLogEntity> oldest = systemLogDbService.lambdaQuery()
                    .orderByAsc(SystemLogEntity::getId)
                    .last("LIMIT " + (count - MAX_LOG))
                    .list();
            if (!oldest.isEmpty()) {
                systemLogDbService.removeByIds(oldest.stream().map(SystemLogEntity::getId).collect(Collectors.toList()));
            }
        }
    }

    public void add(String action, String status) {
        add(action, status, "");
    }
}
