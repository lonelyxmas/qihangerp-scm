package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KnowledgeBaseEntity;
import cn.qihang.ai.assistant.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseDbServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBaseEntity> implements KnowledgeBaseDbService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public KnowledgeBaseDbServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }
}
