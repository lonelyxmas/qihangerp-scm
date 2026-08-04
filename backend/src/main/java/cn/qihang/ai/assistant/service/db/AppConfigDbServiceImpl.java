package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.AppConfigEntity;
import cn.qihang.ai.assistant.mapper.AppConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class AppConfigDbServiceImpl extends ServiceImpl<AppConfigMapper, AppConfigEntity> implements AppConfigDbService {

    @Override
    public String findValueByKey(String key) {
        return baseMapper.findValueByKey(key);
    }

    @Override
    public boolean saveOrUpdateByKey(String key, String value) {
        AppConfigEntity existing = getOne(new LambdaQueryWrapper<AppConfigEntity>()
                .eq(AppConfigEntity::getConfigKey, key));
        if (existing != null) {
            existing.setConfigValue(value);
            return updateById(existing);
        } else {
            AppConfigEntity entity = new AppConfigEntity();
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            return save(entity);
        }
    }
}