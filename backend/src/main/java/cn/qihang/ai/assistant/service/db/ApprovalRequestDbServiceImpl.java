package cn.qihang.ai.assistant.service.db;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.qihang.ai.assistant.entity.ApprovalRequestEntity;
import cn.qihang.ai.assistant.mapper.ApprovalRequestMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ApprovalRequestDbServiceImpl extends ServiceImpl<ApprovalRequestMapper, ApprovalRequestEntity> implements ApprovalRequestDbService {

    private final ApprovalRequestMapper approvalRequestMapper;

    public ApprovalRequestDbServiceImpl(ApprovalRequestMapper approvalRequestMapper) {
        this.approvalRequestMapper = approvalRequestMapper;
    }

    @Override
    public List<ApprovalRequestEntity> listPendingForUser(Long userId) {
        return approvalRequestMapper.listPendingForUser(userId);
    }

    @Override
    public List<ApprovalRequestEntity> listBySubmitter(Long userId) {
        return approvalRequestMapper.listBySubmitter(userId);
    }

    @Override
    public List<ApprovalRequestEntity> listHistoryForUser(Long userId) {
        return approvalRequestMapper.listHistoryForUser(userId);
    }

    @Override
    public List<ApprovalRequestEntity> listAll() {
        return approvalRequestMapper.listAll();
    }

    @Override
    public ApprovalRequestEntity approve(Long id, String comment) {
        ApprovalRequestEntity entity = getById(id);
        if (entity == null) return null;
        entity.setStatus("approved");
        entity.setComment(comment);
        entity.setProcessedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        updateById(entity);
        return entity;
    }

    @Override
    public ApprovalRequestEntity reject(Long id, String comment) {
        ApprovalRequestEntity entity = getById(id);
        if (entity == null) return null;
        entity.setStatus("rejected");
        entity.setComment(comment);
        entity.setProcessedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        updateById(entity);
        return entity;
    }
}
