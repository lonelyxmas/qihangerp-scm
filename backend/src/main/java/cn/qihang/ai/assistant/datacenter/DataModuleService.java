package cn.qihang.ai.assistant.datacenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.qihang.ai.assistant.entity.DataModuleEntity;
import cn.qihang.ai.assistant.entity.DataSetEntity;
import cn.qihang.ai.assistant.mapper.DataModuleMapper;
import cn.qihang.ai.assistant.service.db.DataSetDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataModuleService {

    private static final Logger log = LoggerFactory.getLogger(DataModuleService.class);

    private final DataModuleMapper moduleMapper;
    private final DataSetDbService dataSetDbService;

    public DataModuleService(DataModuleMapper moduleMapper, DataSetDbService dataSetDbService) {
        this.moduleMapper = moduleMapper;
        this.dataSetDbService = dataSetDbService;
    }

    public List<Map<String, Object>> getAllModules() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<DataModuleEntity> modules = moduleMapper.selectList(
            new LambdaQueryWrapper<DataModuleEntity>().orderByAsc(DataModuleEntity::getSortOrder)
        );
        for (DataModuleEntity module : modules) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", module.getModuleId());
            map.put("name", module.getName());
            map.put("description", module.getDescription());
            map.put("icon", module.getIcon());
            map.put("sortOrder", module.getSortOrder());
            map.put("createdAt", module.getCreatedAt());
            map.put("updatedAt", module.getUpdatedAt());

            long datasetCount = dataSetDbService.countByModuleId(module.getModuleId());
            map.put("datasetCount", datasetCount);

            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getModule(String moduleId) {
        LambdaQueryWrapper<DataModuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModuleEntity::getModuleId, moduleId);
        DataModuleEntity entity = moduleMapper.selectOne(wrapper);
        if (entity == null) return null;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getModuleId());
        map.put("name", entity.getName());
        map.put("description", entity.getDescription());
        map.put("icon", entity.getIcon());
        map.put("sortOrder", entity.getSortOrder());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());

        long datasetCount = dataSetDbService.countByModuleId(moduleId);
        map.put("datasetCount", datasetCount);

        return map;
    }

    public Map<String, Object> createModule(String name, String description, String icon) {
        String moduleId = "MOD" + System.currentTimeMillis();
        String now = TimeUtil.nowStr();

        DataModuleEntity entity = new DataModuleEntity();
        entity.setModuleId(moduleId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setIcon(icon != null ? icon : "📦");
        entity.setSortOrder(getNextSortOrder());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        moduleMapper.insert(entity);
        log.info("Created data module: {} ({})", name, moduleId);

        return getModule(moduleId);
    }

    public Map<String, Object> updateModule(String moduleId, String name, String description, String icon, Integer sortOrder) {
        LambdaQueryWrapper<DataModuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModuleEntity::getModuleId, moduleId);
        DataModuleEntity entity = moduleMapper.selectOne(wrapper);
        if (entity == null) return null;

        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (icon != null) entity.setIcon(icon);
        if (sortOrder != null) entity.setSortOrder(sortOrder);
        entity.setUpdatedAt(TimeUtil.nowStr());

        moduleMapper.updateById(entity);
        log.info("Updated data module: {}", moduleId);

        return getModule(moduleId);
    }

    public boolean deleteModule(String moduleId) {
        LambdaQueryWrapper<DataModuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModuleEntity::getModuleId, moduleId);
        int deleted = moduleMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("Deleted data module: {}", moduleId);
            return true;
        }
        return false;
    }

    private int getNextSortOrder() {
        Long count = moduleMapper.selectCount(null);
        return count != null ? count.intValue() : 0;
    }
}
