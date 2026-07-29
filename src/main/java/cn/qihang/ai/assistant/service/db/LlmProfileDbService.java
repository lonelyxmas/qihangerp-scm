package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.LlmProfileEntity;

import java.util.List;

public interface LlmProfileDbService extends IService<LlmProfileEntity> {
    LlmProfileEntity findDefault();
    LlmProfileEntity findByName(String name);
    List<LlmProfileEntity> listAllOrdered();
}
