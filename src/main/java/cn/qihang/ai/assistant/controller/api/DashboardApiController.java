package cn.qihang.ai.assistant.controller.api;

import cn.qihang.ai.assistant.controller.BaseController;
import cn.qihang.ai.assistant.datacenter.DataSetService;
import cn.qihang.ai.assistant.entity.AiAnalysisEntity;
import cn.qihang.ai.assistant.entity.DataSetEntity;
import cn.qihang.ai.assistant.entity.KnowledgeBaseEntity;
import cn.qihang.ai.assistant.entity.NotificationEntity;
import cn.qihang.ai.assistant.model.TaskData.TaskItem;
import cn.qihang.ai.assistant.service.KnowledgeBaseService;
import cn.qihang.ai.assistant.service.TaskService;
import cn.qihang.ai.assistant.service.ReminderService;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.AiAnalysisDbService;
import cn.qihang.ai.assistant.service.db.ApprovalRequestDbService;
import cn.qihang.ai.assistant.service.db.DataSetDbService;
import cn.qihang.ai.assistant.service.db.MessageDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import cn.qihang.ai.assistant.service.db.NoteEmbeddingDbService;
import cn.qihang.ai.assistant.service.db.SessionDbService;
import cn.qihang.ai.assistant.service.db.DataSetRecordDbService;
import cn.qihang.ai.assistant.service.ISysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController extends BaseController {

    private final KnowledgeBaseService kbService;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final DataSetService dataSetService;
    private final ActivityLogDbService activityLogDbService;
    private final AiAnalysisDbService aiAnalysisDbService;
    private final NotificationDbService notificationDbService;
    private final ApprovalRequestDbService approvalRequestDbService;
    private final SessionDbService sessionDbService;
    private final MessageDbService messageDbService;
    private final NoteEmbeddingDbService noteEmbeddingDbService;
    private final DataSetDbService dataSetDbService;
    private final DataSetRecordDbService dataSetRecordDbService;
    private final ISysUserService sysUserService;

    public DashboardApiController(KnowledgeBaseService kbService,
                                  TaskService taskService,
                                  ReminderService reminderService,
                                  DataSetService dataSetService,
                                  ActivityLogDbService activityLogDbService,
                                  AiAnalysisDbService aiAnalysisDbService,
                                  NotificationDbService notificationDbService,
                                  ApprovalRequestDbService approvalRequestDbService,
                                  SessionDbService sessionDbService,
                                  MessageDbService messageDbService,
                                  NoteEmbeddingDbService noteEmbeddingDbService,
                                  DataSetDbService dataSetDbService,
                                  DataSetRecordDbService dataSetRecordDbService,
                                  ISysUserService sysUserService) {
        this.kbService = kbService;
        this.taskService = taskService;
        this.reminderService = reminderService;
        this.dataSetService = dataSetService;
        this.activityLogDbService = activityLogDbService;
        this.aiAnalysisDbService = aiAnalysisDbService;
        this.notificationDbService = notificationDbService;
        this.approvalRequestDbService = approvalRequestDbService;
        this.sessionDbService = sessionDbService;
        this.messageDbService = messageDbService;
        this.noteEmbeddingDbService = noteEmbeddingDbService;
        this.dataSetDbService = dataSetDbService;
        this.dataSetRecordDbService = dataSetRecordDbService;
        this.sysUserService = sysUserService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 1. 业务数据概览
            Map<String, Object> businessStats = getBusinessStats();
            result.put("businessStats", businessStats);

            // 2. 最近任务和提醒
            result.put("recentTasks", getRecentTasks());
            result.put("overdueTasks", getOverdueTasks());
            result.put("reminders", getReminderData());

            // 3. 数据集最近情况
            result.put("recentDatasets", getRecentDatasets());

            // 4. 最近的日报
            result.put("recentReports", getRecentReports());

            // 5. 最新动态
            result.put("recentActivity", getRecentActivity());

            // 6. 未读通知 & 待审批
            result.put("unreadNotifications", getUnreadNotifications());
            result.put("pendingApprovals", getPendingApprovals());

            result.put("ok", true);
        } catch (Exception e) {
            log.error("获取看板数据失败", e);
            result.put("ok", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> getBusinessStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            List<KnowledgeBaseEntity> kbs = kbService.getAll();
            stats.put("knowledgeBases", Map.of("count", kbs.size(), "label", "知识库"));

            long fileCount = 0;
            long chunkCount = 0;
            for (KnowledgeBaseEntity kb : kbs) {
                try {
                    fileCount += noteEmbeddingDbService.countFilesByKb(kb.getId());
                    chunkCount += noteEmbeddingDbService.countByKb(kb.getId());
                } catch (Exception ignored) {}
            }
            stats.put("indexedFiles", Map.of("count", fileCount, "label", "索引文件"));
            stats.put("indexedChunks", Map.of("count", chunkCount, "label", "索引分块"));

            long sessionCount = 0;
            long msgCount = 0;
            try {
                sessionCount = sessionDbService.count();
                msgCount = messageDbService.count();
            } catch (Exception ignored) {}
            stats.put("chatSessions", Map.of("count", sessionCount, "label", "对话会话"));
            stats.put("chatMessages", Map.of("count", msgCount, "label", "对话消息"));

            long datasetCount = 0;
            long recordCount = 0;
            try {
                datasetCount = dataSetDbService.count();
                List<DataSetEntity> datasets = dataSetDbService.list();
                for (DataSetEntity ds : datasets) {
                    try {
                        recordCount += dataSetRecordDbService.countByDataset(ds.getDatasetId());
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            stats.put("dataSets", Map.of("count", datasetCount, "label", "数据集"));
            stats.put("dataRecords", Map.of("count", recordCount, "label", "数据记录"));

            List<TaskItem> allTasks = Collections.emptyList();
            try {
                allTasks = taskService.getAllTasks();
            } catch (Exception ignored) {}
            long pending = allTasks.stream().filter(t -> "pending".equals(t.status)).count();
            long inProgress = allTasks.stream().filter(t -> "in_progress".equals(t.status)).count();
            long done = allTasks.stream().filter(t -> "done".equals(t.status)).count();
            stats.put("tasks", Map.of("total", allTasks.size(), "pending", pending, "inProgress", inProgress, "done", done, "label", "任务"));

            long reminderCount = 0;
            try {
                reminderCount = reminderService.getAllReminders().size();
            } catch (Exception ignored) {}
            stats.put("reminders", Map.of("count", reminderCount, "label", "提醒"));

            long totalActivity = 0;
            try {
                totalActivity = activityLogDbService.count();
            } catch (Exception ignored) {}
            stats.put("totalActivity", Map.of("count", totalActivity, "label", "活动记录"));

            long userCount = 0;
            try {
                userCount = sysUserService.selectUserList(null).size();
            } catch (Exception ignored) {}
            stats.put("users", Map.of("count", userCount, "label", "用户"));

            try {
                Long userId = getUserId();
                int unread = notificationDbService.countUnread(userId);
                stats.put("unreadNotifications", unread);
                int pendingCount = approvalRequestDbService.listPendingForUser(userId).size();
                stats.put("pendingApprovals", pendingCount);
            } catch (Exception ignored) {}

        } catch (Exception e) {
            log.error("获取业务统计失败", e);
        }
        return stats;
    }

    private List<Map<String, Object>> getRecentTasks() {
        try {
            return taskService.getAllTasks().stream()
                    .filter(t -> "pending".equals(t.status) || "in_progress".equals(t.status))
                    .sorted((a, b) -> {
                        if (a.dueDate != null && b.dueDate != null) return a.dueDate.compareTo(b.dueDate);
                        if (a.dueDate != null) return -1;
                        if (b.dueDate != null) return 1;
                        return b.createdAt.compareTo(a.createdAt);
                    })
                    .limit(10)
                    .map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", t.id);
                        m.put("title", t.title);
                        m.put("status", t.status);
                        m.put("priority", t.priority);
                        m.put("dueDate", t.dueDate);
                        m.put("createdAt", t.createdAt);
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取最近任务失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getOverdueTasks() {
        try {
            String today = LocalDate.now().toString();
            return taskService.getAllTasks().stream()
                    .filter(t -> ("pending".equals(t.status) || "in_progress".equals(t.status))
                            && t.dueDate != null && t.dueDate.compareTo(today) < 0)
                    .sorted((a, b) -> a.dueDate.compareTo(b.dueDate))
                    .limit(5)
                    .map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", t.id);
                        m.put("title", t.title);
                        m.put("dueDate", t.dueDate);
                        m.put("priority", t.priority);
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取过期任务失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getReminderData() {
        try {
            return reminderService.getAllReminders().stream()
                    .filter(r -> r.enabled)
                    .sorted((a, b) -> {
                        if (a.time != null && b.time != null) return a.time.compareTo(b.time);
                        return 0;
                    })
                    .limit(10)
                    .map(r -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", r.id);
                        m.put("name", r.name);
                        m.put("message", r.message);
                        m.put("type", r.type);
                        m.put("time", r.time);
                        m.put("description", reminderService.getReminderDescription(r));
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取提醒失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getRecentDatasets() {
        try {
            return dataSetService.getAllDatasets().stream()
                    .sorted((a, b) -> {
                        if (b.getUpdatedAt() != null && a.getUpdatedAt() != null)
                            return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                        return 0;
                    })
                    .limit(10)
                    .map(ds -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", ds.getId());
                        m.put("name", ds.getName());
                        m.put("type", ds.getType());
                        m.put("status", ds.getStatus());
                        m.put("recordCount", ds.getRecordCount());
                        m.put("moduleId", ds.getModuleId());
                        m.put("createdAt", ds.getCreatedAt());
                        m.put("updatedAt", ds.getUpdatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取最近数据集失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getRecentReports() {
        try {
            List<KnowledgeBaseEntity> kbs = kbService.getAll();
            List<Map<String, Object>> reports = new ArrayList<>();
            for (KnowledgeBaseEntity kb : kbs) {
                try {
                    AiAnalysisEntity report = aiAnalysisDbService.getLatestReport(kb.getId());
                    if (report != null) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("kbId", kb.getId());
                        m.put("kbName", kb.getName());
                        m.put("reportDate", report.getReportDate());
                        m.put("content", report.getContent());
                        m.put("createdAt", report.getCreatedAt());
                        reports.add(m);
                    } else {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("kbId", kb.getId());
                        m.put("kbName", kb.getName());
                        m.put("reportDate", null);
                        m.put("content", null);
                        m.put("createdAt", null);
                        reports.add(m);
                    }
                } catch (Exception ignored) {}
            }
            reports.sort((a, b) -> {
                String da = (String) a.get("reportDate");
                String db = (String) b.get("reportDate");
                if (da != null && db != null) return db.compareTo(da);
                if (da != null) return -1;
                if (db != null) return 1;
                return 0;
            });
            return reports;
        } catch (Exception e) {
            log.error("获取最近日报失败", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getRecentActivity() {
        try {
            return activityLogDbService.listRecent(15).stream()
                    .map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", a.getId());
                        m.put("actionType", a.getActionType());
                        m.put("actionDesc", a.getActionDesc());
                        m.put("source", a.getSource());
                        m.put("triggeredName", a.getTriggeredName());
                        m.put("createdAt", a.getCreatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取最近动态失败", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> getUnreadNotifications() {
        try {
            Long userId = getUserId();
            int unreadCount = notificationDbService.countUnread(userId);
            List<NotificationEntity> recent = notificationDbService.listByUser(userId, 5);
            List<Map<String, Object>> list = recent.stream().map(n -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", n.getId());
                m.put("title", n.getTitle());
                m.put("content", n.getContent());
                m.put("type", n.getType());
                m.put("isRead", n.getIsRead());
                m.put("createdAt", n.getCreatedAt());
                return m;
            }).collect(Collectors.toList());
            return Map.of("unreadCount", unreadCount, "list", list);
        } catch (Exception e) {
            log.error("获取通知失败", e);
            return Map.of("unreadCount", 0, "list", Collections.emptyList());
        }
    }

    private Map<String, Object> getPendingApprovals() {
        try {
            Long userId = getUserId();
            var pending = approvalRequestDbService.listPendingForUser(userId);
            return Map.of("count", pending.size(),
                    "list", pending.stream().map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", a.getId());
                        m.put("title", a.getTitle());
                        m.put("submitterName", a.getSubmitterName());
                        m.put("createdAt", a.getCreatedAt());
                        return m;
                    }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("获取待审批失败", e);
            return Map.of("count", 0, "list", Collections.emptyList());
        }
    }
}
