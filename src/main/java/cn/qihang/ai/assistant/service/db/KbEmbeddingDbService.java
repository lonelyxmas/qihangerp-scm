package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.KbEmbeddingEntity;
import cn.qihang.ai.assistant.mapper.KbEmbeddingMapper;
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
}