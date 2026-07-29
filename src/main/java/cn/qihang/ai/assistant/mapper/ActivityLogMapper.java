package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.ActivityLogEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ActivityLogMapper extends BaseMapper<ActivityLogEntity> {

    @Select("SELECT * FROM activity_log ORDER BY created_at DESC LIMIT #{limit}")
    List<ActivityLogEntity> listRecent(int limit);

    @Select("SELECT * FROM activity_log WHERE action_type = #{type} ORDER BY created_at DESC LIMIT #{limit}")
    List<ActivityLogEntity> listByType(String type, int limit);
}
