package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.MessageEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MessageMapper extends BaseMapper<MessageEntity> {

    @Select("SELECT * FROM ai_messages WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<MessageEntity> listBySession(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM ai_messages WHERE session_id = #{sessionId} AND mode = #{mode} ORDER BY created_at ASC")
    List<MessageEntity> listBySessionAndMode(@Param("sessionId") String sessionId, @Param("mode") String mode);

    @Select("SELECT * FROM ai_messages WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<MessageEntity> listRecentBySession(@Param("sessionId") String sessionId, @Param("limit") int limit);

    @Select("SELECT * FROM ai_messages WHERE kb_id = #{kbId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<MessageEntity> listByKb(@Param("kbId") Long kbId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM ai_messages WHERE kb_id = #{kbId}")
    long countByKb(@Param("kbId") Long kbId);

    @Select("SELECT * FROM ai_messages WHERE kb_id = #{kbId} AND content LIKE '%' || #{q} || '%' ORDER BY created_at DESC LIMIT #{limit}")
    List<MessageEntity> searchByKb(@Param("kbId") Long kbId, @Param("q") String q, @Param("limit") int limit);
}
