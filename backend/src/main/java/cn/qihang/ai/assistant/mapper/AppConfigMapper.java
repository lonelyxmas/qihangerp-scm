package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.AppConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppConfigMapper extends BaseMapper<AppConfigEntity> {

    @Select("SELECT config_value FROM app_config WHERE config_key = #{key}")
    String findValueByKey(String key);
}