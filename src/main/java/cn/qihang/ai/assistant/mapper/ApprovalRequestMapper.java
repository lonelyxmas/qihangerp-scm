package cn.qihang.ai.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.qihang.ai.assistant.entity.ApprovalRequestEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ApprovalRequestMapper extends BaseMapper<ApprovalRequestEntity> {

    @Select("SELECT * FROM approval_requests WHERE approver_id = #{userId} AND status = 'pending' ORDER BY created_at DESC")
    List<ApprovalRequestEntity> listPendingForUser(Long userId);

    @Select("SELECT * FROM approval_requests WHERE submitter_id = #{userId} ORDER BY created_at DESC")
    List<ApprovalRequestEntity> listBySubmitter(Long userId);

    @Select("SELECT * FROM approval_requests WHERE approver_id = #{userId} AND status != 'pending' ORDER BY created_at DESC")
    List<ApprovalRequestEntity> listHistoryForUser(Long userId);

    @Select("SELECT * FROM approval_requests ORDER BY created_at DESC")
    List<ApprovalRequestEntity> listAll();
}
