package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.DataSetRecordEntity;
import cn.qihang.ai.assistant.mapper.DataSetRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSetRecordDbServiceImpl extends ServiceImpl<DataSetRecordMapper, DataSetRecordEntity> implements DataSetRecordDbService {

    private final DataSetRecordMapper recordMapper;

    public DataSetRecordDbServiceImpl(DataSetRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    @Override
    public List<DataSetRecordEntity> listByDataset(String datasetId) {
        return recordMapper.listByDataset(datasetId);
    }

    @Override
    public List<DataSetRecordEntity> listByDataset(String datasetId, int page, int size) {
        return recordMapper.listByDatasetPage(datasetId, page, size);
    }

    @Override
    public int countByDataset(String datasetId) {
        return recordMapper.countByDataset(datasetId);
    }

    @Override
    public boolean existsByHash(String datasetId, String hash) {
        return recordMapper.findByHash(datasetId, hash) != null;
    }
}
