package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.DataSetRecordEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DataSetRecordMapper extends BaseMapper<DataSetRecordEntity> {

    @Select("SELECT * FROM data_center_records WHERE dataset_id = #{datasetId} AND content_hash = #{hash} LIMIT 1")
    DataSetRecordEntity findByHash(@Param("datasetId") String datasetId, @Param("hash") String hash);

    @Select("SELECT * FROM data_center_records WHERE dataset_id = #{datasetId} ORDER BY id DESC")
    List<DataSetRecordEntity> listByDataset(@Param("datasetId") String datasetId);

    @Select("SELECT * FROM data_center_records WHERE dataset_id = #{datasetId} ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<DataSetRecordEntity> listByDatasetPage(@Param("datasetId") String datasetId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM data_center_records WHERE dataset_id = #{datasetId}")
    int countByDataset(@Param("datasetId") String datasetId);
}
