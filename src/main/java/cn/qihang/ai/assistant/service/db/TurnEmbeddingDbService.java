package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.TurnEmbeddingEntity;

import java.util.List;

public interface TurnEmbeddingDbService extends IService<TurnEmbeddingEntity> {
    List<TurnEmbeddingEntity> listBySession(String sessionId);
    int maxTurnOrder(String sessionId);
    int countBySession(String sessionId);
}
