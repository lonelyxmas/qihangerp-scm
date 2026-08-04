package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.ActivityLogEntity;
import cn.qihang.ai.assistant.mapper.ActivityLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityLogDbServiceImpl extends ServiceImpl<ActivityLogMapper, ActivityLogEntity> implements ActivityLogDbService {

    private final ActivityLogMapper activityLogMapper;

    public ActivityLogDbServiceImpl(ActivityLogMapper activityLogMapper) {
        this.activityLogMapper = activityLogMapper;
    }

    @Override
    public List<ActivityLogEntity> listRecent(int limit) {
        return activityLogMapper.listRecent(limit);
    }

    @Override
    public List<ActivityLogEntity> listByType(String type, int limit) {
        return activityLogMapper.listByType(type, limit);
    }

    @Override
    public void addLog(String actionType, String actionDesc, String source, Long triggeredBy, String triggeredName) {
        ActivityLogEntity log = new ActivityLogEntity();
        log.setActionType(actionType);
        log.setActionDesc(actionDesc);
        log.setSource(source);
        log.setTriggeredBy(triggeredBy);
        log.setTriggeredName(triggeredName);
        log.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        save(log);
    }
}
