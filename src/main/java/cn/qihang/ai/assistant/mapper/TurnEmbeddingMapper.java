package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.TurnEmbeddingEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TurnEmbeddingMapper extends BaseMapper<TurnEmbeddingEntity> {

    @Select("SELECT * FROM ai_turn_embeddings WHERE session_id = #{sessionId} ORDER BY turn_order ASC")
    List<TurnEmbeddingEntity> listBySession(@Param("sessionId") String sessionId);

    @Select("SELECT COALESCE(MAX(turn_order), -1) FROM ai_turn_embeddings WHERE session_id = #{sessionId}")
    int maxTurnOrder(@Param("sessionId") String sessionId);

    @Select("SELECT COUNT(*) FROM ai_turn_embeddings WHERE session_id = #{sessionId}")
    int countBySession(@Param("sessionId") String sessionId);
}
