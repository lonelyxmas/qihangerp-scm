package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.entity.ActivityLogEntity;
import cn.qihang.ai.assistant.entity.ApprovalRequestEntity;
import cn.qihang.ai.assistant.entity.NotificationEntity;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.ApprovalRequestDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/collab")
public class CollaborationApiController {

    private final ActivityLogDbService activityLogDbService;
    private final NotificationDbService notificationDbService;
    private final ApprovalRequestDbService approvalRequestDbService;

    public CollaborationApiController(ActivityLogDbService activityLogDbService,
                                      NotificationDbService notificationDbService,
                                      ApprovalRequestDbService approvalRequestDbService) {
        this.activityLogDbService = activityLogDbService;
        this.notificationDbService = notificationDbService;
        this.approvalRequestDbService = approvalRequestDbService;
    }

    // ==================== 活动日志 API ====================

    @GetMapping("/activity/list")
    @ResponseBody
    public Map<String, Object> listActivity(@RequestParam(defaultValue = "50") int limit,
                                            @RequestParam(required = false) String type) {
        try {
            List<ActivityLogEntity> list;
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                list = activityLogDbService.listByType(type, limit);
            } else {
                list = activityLogDbService.listRecent(limit);
            }
            return Map.of("ok", true, "data", list.stream().map(this::toActivityMap).toList());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ==================== 通知 API ====================

    @GetMapping("/notification/list")
    @ResponseBody
    public Map<String, Object> listNotifications(@RequestParam(defaultValue = "1") Long userId,
                                                 @RequestParam(defaultValue = "50") int limit,
                                                 @RequestParam(required = false) String type) {
        try {
            List<NotificationEntity> list;
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                list = notificationDbService.listByUserAndType(userId, type, limit);
            } else {
                list = notificationDbService.listByUser(userId, limit);
            }
            int unreadCount = notificationDbService.countUnread(userId);
            return Map.of("ok", true, "data", list.stream().map(this::toNotificationMap).toList(),
                    "unreadCount", unreadCount);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/notification/mark-read")
    @ResponseBody
    public Map<String, Object> markNotificationRead(@RequestParam Long id) {
        try {
            NotificationEntity entity = notificationDbService.getById(id);
            if (entity != null) {
                entity.setIsRead(1);
                entity.setReadAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                notificationDbService.updateById(entity);
            }
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/notification/mark-all-read")
    @ResponseBody
    public Map<String, Object> markAllRead(@RequestParam Long userId) {
        try {
            notificationDbService.markAllRead(userId);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ==================== 审批 API ====================

    @GetMapping("/approval/list")
    @ResponseBody
    public Map<String, Object> listApprovals(@RequestParam(defaultValue = "1") Long userId,
                                             @RequestParam String tab) {
        try {
            List<ApprovalRequestEntity> list;
            switch (tab) {
                case "pending":
                    list = approvalRequestDbService.listPendingForUser(userId);
                    break;
                case "submitted":
                    list = approvalRequestDbService.listBySubmitter(userId);
                    break;
                case "history":
                    list = approvalRequestDbService.listHistoryForUser(userId);
                    break;
                default:
                    list = approvalRequestDbService.listAll();
            }
            return Map.of("ok", true, "data", list.stream().map(this::toApprovalMap).toList());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/approval/submit")
    @ResponseBody
    public Map<String, Object> submitApproval(@RequestParam String title,
                                              @RequestParam(required = false) String description,
                                              @RequestParam String sourceType,
                                              @RequestParam String sourceId,
                                              @RequestParam Long submitterId,
                                              @RequestParam(required = false) String submitterName,
                                              @RequestParam Long approverId,
                                              @RequestParam(required = false) String approverName) {
        try {
            ApprovalRequestEntity entity = new ApprovalRequestEntity();
            entity.setRequestId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            entity.setTitle(title);
            entity.setDescription(description);
            entity.setSourceType(sourceType);
            entity.setSourceId(sourceId);
            entity.setSubmitterId(submitterId);
            entity.setSubmitterName(submitterName);
            entity.setApproverId(approverId);
            entity.setApproverName(approverName);
            entity.setStatus("pending");
            entity.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            approvalRequestDbService.save(entity);
            return Map.of("ok", true, "data", toApprovalMap(entity));
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/approval/process")
    @ResponseBody
    public Map<String, Object> processApproval(@RequestParam Long id,
                                               @RequestParam String action,
                                               @RequestParam(required = false) String comment) {
        try {
            ApprovalRequestEntity result;
            if ("approve".equals(action)) {
                result = approvalRequestDbService.approve(id, comment);
            } else if ("reject".equals(action)) {
                result = approvalRequestDbService.reject(id, comment);
            } else {
                return Map.of("ok", false, "error", "无效操作: " + action);
            }
            if (result == null) {
                return Map.of("ok", false, "error", "审批请求不存在");
            }
            return Map.of("ok", true, "data", toApprovalMap(result));
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ==================== 转换辅助 ====================

    private Map<String, Object> toActivityMap(ActivityLogEntity e) {
        return Map.of(
                "id", e.getId(),
                "actionType", e.getActionType(),
                "actionDesc", e.getActionDesc(),
                "source", e.getSource(),
                "triggeredName", e.getTriggeredName() != null ? e.getTriggeredName() : "",
                "targetName", e.getTargetName() != null ? e.getTargetName() : "",
                "createdAt", e.getCreatedAt() != null ? e.getCreatedAt() : ""
        );
    }

    private Map<String, Object> toNotificationMap(NotificationEntity e) {
        return Map.of(
                "id", e.getId(),
                "title", e.getTitle(),
                "content", e.getContent(),
                "type", e.getType(),
                "isRead", e.getIsRead() != null ? e.getIsRead() : 0,
                "createdAt", e.getCreatedAt() != null ? e.getCreatedAt() : ""
        );
    }

    private Map<String, Object> toApprovalMap(ApprovalRequestEntity e) {
        return Map.of(
                "id", e.getId(),
                "requestId", e.getRequestId(),
                "title", e.getTitle(),
                "description", e.getDescription() != null ? e.getDescription() : "",
                "sourceType", e.getSourceType(),
                "submitterName", e.getSubmitterName() != null ? e.getSubmitterName() : "",
                "approverName", e.getApproverName() != null ? e.getApproverName() : "",
                "status", e.getStatus(),
                "comment", e.getComment() != null ? e.getComment() : "",
                "createdAt", e.getCreatedAt() != null ? e.getCreatedAt() : ""
        );
    }
}
