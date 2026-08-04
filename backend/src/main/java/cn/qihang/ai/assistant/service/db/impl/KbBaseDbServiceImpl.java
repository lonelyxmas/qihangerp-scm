package cn.qihang.ai.assistant.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.mapper.KbBaseMapper;
import cn.qihang.ai.assistant.service.db.KbBaseDbService;
import org.springframework.stereotype.Service;

@Service
public class KbBaseDbServiceImpl extends ServiceImpl<KbBaseMapper, KbBaseEntity> implements KbBaseDbService {

    private final KbBaseMapper kbBaseMapper;

    public KbBaseDbServiceImpl(KbBaseMapper kbBaseMapper) {
        this.kbBaseMapper = kbBaseMapper;
    }
}