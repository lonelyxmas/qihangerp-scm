package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface LlmProfileMapper extends BaseMapper<LlmProfileEntity> {

    @Select("SELECT * FROM ai_llm_profiles WHERE is_default = 1 LIMIT 1")
    LlmProfileEntity findDefault();

    @Select("SELECT * FROM ai_llm_profiles WHERE name = #{name} LIMIT 1")
    LlmProfileEntity findByName(String name);

    @Select("SELECT * FROM ai_llm_profiles ORDER BY is_default DESC, id ASC")
    List<LlmProfileEntity> listAllOrdered();
}
