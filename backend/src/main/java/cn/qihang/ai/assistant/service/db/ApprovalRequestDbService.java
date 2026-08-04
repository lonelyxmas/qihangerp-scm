package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.qihang.ai.assistant.entity.ApprovalRequestEntity;

import java.util.List;

public interface ApprovalRequestDbService extends IService<ApprovalRequestEntity> {
    List<ApprovalRequestEntity> listPendingForUser(Long userId);
    List<ApprovalRequestEntity> listBySubmitter(Long userId);
    List<ApprovalRequestEntity> listHistoryForUser(Long userId);
    List<ApprovalRequestEntity> listAll();
    ApprovalRequestEntity approve(Long id, String comment);
    ApprovalRequestEntity reject(Long id, String comment);
}
