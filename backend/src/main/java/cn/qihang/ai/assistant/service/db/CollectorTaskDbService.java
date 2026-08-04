package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.CollectorTaskEntity;

import java.util.List;

public interface CollectorTaskDbService extends IService<CollectorTaskEntity> {
    CollectorTaskEntity getByTaskId(String taskId);
    List<CollectorTaskEntity> getAllEnabled();
}
