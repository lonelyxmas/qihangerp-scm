package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.DataSetEntity;

public interface DataSetDbService extends IService<DataSetEntity> {
    long countByModuleId(String moduleId);
}
