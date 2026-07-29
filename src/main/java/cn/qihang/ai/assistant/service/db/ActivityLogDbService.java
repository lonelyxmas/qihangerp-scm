package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.ActivityLogEntity;

import java.util.List;

public interface ActivityLogDbService extends IService<ActivityLogEntity> {
    List<ActivityLogEntity> listRecent(int limit);
    List<ActivityLogEntity> listByType(String type, int limit);
    void addLog(String actionType, String actionDesc, String source, Long triggeredBy, String triggeredName);
}
