package cn.qihang.ai.assistant.service.db.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KbNoteEntity;
import cn.qihang.ai.assistant.mapper.KbNoteMapper;
import cn.qihang.ai.assistant.service.db.KbNoteDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KbNoteDbServiceImpl extends ServiceImpl<KbNoteMapper, KbNoteEntity> implements KbNoteDbService {

    private static final Logger log = LoggerFactory.getLogger(KbNoteDbServiceImpl.class);

    @Override
    public List<KbNoteEntity> listByKbId(Long kbId) {
        return lambdaQuery()
                .eq(KbNoteEntity::getKbId, kbId)
                .orderByAsc(KbNoteEntity::getPath)
                .list();
    }

    @Override
    public KbNoteEntity getByKbIdAndPath(Long kbId, String path) {
        return lambdaQuery()
                .eq(KbNoteEntity::getKbId, kbId)
                .eq(KbNoteEntity::getPath, path)
                .one();
    }

    @Override
    public void deleteByKbIdAndPath(Long kbId, String path) {
        lambdaUpdate()
                .eq(KbNoteEntity::getKbId, kbId)
                .eq(KbNoteEntity::getPath, path)
                .remove();
    }

    @Override
    public void deleteByKbId(Long kbId) {
        lambdaUpdate()
                .eq(KbNoteEntity::getKbId, kbId)
                .remove();
    }
}