package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.SessionEntity;

import java.util.List;

public interface SessionDbService extends IService<SessionEntity> {

    SessionEntity findLatestByKb(Long kbId);
    List<SessionEntity> listByKb(Long kbId);
    List<SessionEntity> listAllOrderByUpdate();
    List<SessionEntity> listBySourceOrderByUpdate(String source);

}
