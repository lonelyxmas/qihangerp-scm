package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.MessageEntity;

import java.util.List;

public interface MessageDbService extends IService<MessageEntity> {
    List<MessageEntity> listBySession(String sessionId);
    List<MessageEntity> listBySessionAndMode(String sessionId, String mode);
    List<MessageEntity> listRecentBySession(String sessionId, int limit);
    List<MessageEntity> listByKb(Long kbId, int offset, int limit);
    long countByKb(Long kbId);
    List<MessageEntity> searchByKb(Long kbId, String q, int limit);
}
