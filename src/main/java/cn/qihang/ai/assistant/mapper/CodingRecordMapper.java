package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.CodingRecordEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CodingRecordMapper extends BaseMapper<CodingRecordEntity> {

    @Select("SELECT * FROM coding_records ORDER BY id DESC LIMIT #{limit}")
    List<CodingRecordEntity> findRecent(int limit);
}