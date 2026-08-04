package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.NotificationEntity;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface NotificationMapper extends BaseMapper<NotificationEntity> {

    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<NotificationEntity> listByUser(Long userId, int limit);

    @Select("SELECT * FROM notifications WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC LIMIT #{limit}")
    List<NotificationEntity> listByUserAndType(Long userId, String type, int limit);

    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(Long userId);

    @Update("UPDATE notifications SET is_read = 1, read_at = #{readAt} WHERE user_id = #{userId} AND is_read = 0")
    void markAllRead(Long userId, String readAt);
}
