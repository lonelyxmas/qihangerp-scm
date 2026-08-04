package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import cn.qihang.ai.assistant.mapper.LlmProfileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LlmProfileDbServiceImpl extends ServiceImpl<LlmProfileMapper, LlmProfileEntity> implements LlmProfileDbService {

    private final LlmProfileMapper mapper;

    public LlmProfileDbServiceImpl(LlmProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LlmProfileEntity findDefault() {
        return mapper.findDefault();
    }

    @Override
    public LlmProfileEntity findByName(String name) {
        return mapper.findByName(name);
    }

    @Override
    public List<LlmProfileEntity> listAllOrdered() {
        return mapper.listAllOrdered();
    }
}
