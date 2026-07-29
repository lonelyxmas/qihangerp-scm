package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.DataSetRecordEntity;

import java.util.List;

public interface DataSetRecordDbService extends IService<DataSetRecordEntity> {
    List<DataSetRecordEntity> listByDataset(String datasetId);
    List<DataSetRecordEntity> listByDataset(String datasetId, int page, int size);
    int countByDataset(String datasetId);
    boolean existsByHash(String datasetId, String hash);
}
