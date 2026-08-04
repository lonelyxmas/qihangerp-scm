package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.AppConfigEntity;

public interface AppConfigDbService extends IService<AppConfigEntity> {
    String findValueByKey(String key);
    boolean saveOrUpdateByKey(String key, String value);
}