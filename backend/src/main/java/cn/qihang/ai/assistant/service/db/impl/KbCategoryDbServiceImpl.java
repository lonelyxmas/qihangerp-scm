package cn.qihang.ai.assistant.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KbCategoryEntity;
import cn.qihang.ai.assistant.mapper.KbCategoryMapper;
import cn.qihang.ai.assistant.service.db.KbCategoryDbService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KbCategoryDbServiceImpl extends ServiceImpl<KbCategoryMapper, KbCategoryEntity> implements KbCategoryDbService {
    @Override
    public List<KbCategoryEntity> listByKbId(Long kbId) {
        return lambdaQuery()
                .eq(KbCategoryEntity::getKbId, kbId)
                .orderByAsc(KbCategoryEntity::getSortOrder)
                .list();
    }
}
