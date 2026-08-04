package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.mapper.KbEmbeddingMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KbEmbeddingDbService extends ServiceImpl<KbEmbeddingMapper, KbEmbeddingEntity> {

    public int countByKb(Long kbId) {
        long count = lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .count();
        return (int) count;
    }

    public int countFilesByKb(Long kbId) {
        long count = lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .select(KbEmbeddingEntity::getFilePath)
                .list()
                .stream()
                .map(KbEmbeddingEntity::getFilePath)
                .distinct()
                .count();
        return (int) count;
    }

    public void deleteByKbAndPath(Long kbId, String filePath) {
        lambdaUpdate()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .eq(KbEmbeddingEntity::getFilePath, filePath)
                .remove();
    }

    public void deleteByKb(Long kbId) {
        lambdaUpdate()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .remove();
    }

    public KbEmbeddingEntity findByKbAndPathAndChunk(Long kbId, String filePath, Integer chunkIndex) {
        return lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .eq(KbEmbeddingEntity::getFilePath, filePath)
                .eq(KbEmbeddingEntity::getChunkIndex, chunkIndex)
                .one();
    }

    public List<KbEmbeddingEntity> listByKbAndPath(Long kbId, String filePath) {
        return lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .eq(KbEmbeddingEntity::getFilePath, filePath)
                .orderByAsc(KbEmbeddingEntity::getChunkIndex)
                .list();
    }

    public int countByKbAndPath(Long kbId, String filePath) {
        long count = lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId)
                .eq(KbEmbeddingEntity::getFilePath, filePath)
                .count();
        return (int) count;
    }

    public void deleteByNoteId(Long noteId) {
        lambdaUpdate()
                .eq(KbEmbeddingEntity::getNoteId, noteId)
                .remove();
    }

    public List<KbEmbeddingEntity> listByNoteId(Long noteId) {
        return lambdaQuery()
                .eq(KbEmbeddingEntity::getNoteId, noteId)
                .orderByAsc(KbEmbeddingEntity::getChunkIndex)
                .list();
    }
}