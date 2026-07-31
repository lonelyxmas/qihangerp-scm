package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.model.TaskData.*;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final String DONE_DATASET_NAME = "任务完成记录";

    private final DataSource dataSource;
    private final ActivityLogDbService activityLogDbService;
    private final NotificationDbService notificationDbService;
    private final cn.qihang.ai.assistant.datacenter.DataSetService dataSetService;

    public TaskService(DataSource dataSource,
                       ActivityLogDbService activityLogDbService,
                       NotificationDbService notificationDbService,
                       cn.qihang.ai.assistant.datacenter.DataSetService dataSetService) {
        this.dataSource = dataSource;
        this.activityLogDbService = activityLogDbService;
        this.notificationDbService = notificationDbService;
        this.dataSetService = dataSetService;
    }


    // ========== SQLite CRUD ==========

    private List<TaskItem> getAllTasksFromDb() {
        List<TaskItem> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM tasks ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            log.error("[任务] 查询失败", e);
        }
        return tasks;
    }

    private void insertTaskToDb(TaskItem task, Long kbId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tasks (title, description, status, priority, due_date, created_at, updated_at, kb_id, action, action_prompt, last_reminded, scheduled_start, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.title);
            ps.setString(2, task.description);
            ps.setString(3, task.status);
            ps.setString(4, task.priority);
            ps.setString(5, task.dueDate);
            ps.setString(6, task.createdAt);
            ps.setString(7, task.updatedAt);
            if (kbId != null) {
                ps.setLong(8, kbId);
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, task.action);
            ps.setString(10, task.actionPrompt);
            ps.setString(11, task.lastReminded);
            ps.setString(12, task.scheduledStart);
            if (task.createdBy != null) {
                ps.setLong(13, task.createdBy);
            } else {
                ps.setNull(13, Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.id = keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("[任务] 插入失败", e);
        }
    }

    private TaskItem mapRowToTask(ResultSet rs) throws SQLException {
        TaskItem task = new TaskItem();
        task.id = rs.getLong("id");
        task.title = rs.getString("title");
        task.description = rs.getString("description");
        task.status = rs.getString("status");
        task.priority = rs.getString("priority");
        task.dueDate = rs.getString("due_date");
        task.createdAt = rs.getString("created_at");
        task.updatedAt = rs.getString("updated_at");
        task.action = rs.getString("action");
        task.actionPrompt = rs.getString("action_prompt");
        task.lastReminded = rs.getString("last_reminded");
        task.scheduledStart = rs.getString("scheduled_start");
        long createdBy = rs.getLong("created_by");
        task.createdBy = rs.wasNull() ? null : createdBy;
        long kbId = rs.getLong("kb_id");
        task.kbId = rs.wasNull() ? null : kbId;
        return task;
    }

    private TaskItem getTaskFromDb(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToTask(rs);
            }
        } catch (SQLException e) {
            log.error("[任务] 查询失败", e);
        }
        return null;
    }

    private void updateTaskInDb(TaskItem task) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE tasks SET title=?, description=?, status=?, priority=?, due_date=?, updated_at=?, action=?, action_prompt=?, last_reminded=?, scheduled_start=?, created_by=? WHERE id=?")) {
            ps.setString(1, task.title);
            ps.setString(2, task.description);
            ps.setString(3, task.status);
            ps.setString(4, task.priority);
            ps.setString(5, task.dueDate);
            ps.setString(6, task.updatedAt);
            ps.setString(7, task.action);
            ps.setString(8, task.actionPrompt);
            ps.setString(9, task.lastReminded);
            ps.setString(10, task.scheduledStart);
            if (task.createdBy != null) {
                ps.setLong(11, task.createdBy);
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            ps.setLong(12, task.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 更新失败", e);
        }
    }

    private void deleteTaskFromDb(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 删除失败", e);
        }
    }

    // ========== Public API ==========

    public List<TaskItem> getAllTasks() {
        return getAllTasksFromDb();
    }

    /**
     * 按登录用户获取任务：普通用户只能看到自己创建的任务，管理员可见全部。
     */
    public List<TaskItem> getAllTasksForUser(Long userId, boolean admin) {
        List<TaskItem> all = getAllTasksFromDb();
        if (admin || userId == null) {
            return all;
        }
        return all.stream()
                .filter(t -> userId.equals(t.createdBy))
                .collect(java.util.stream.Collectors.toList());
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate) {
        return addTaskInternal(title, description, priority, dueDate, null, null, null, null, null);
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate, Long kbId) {
        return addTaskInternal(title, description, priority, dueDate, kbId, null, null, null, null);
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate, Long kbId,
                            String action, String actionPrompt) {
        return addTaskInternal(title, description, priority, dueDate, kbId, action, actionPrompt, null, null);
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate, Long kbId,
                            String action, String actionPrompt, String scheduledStart, Long createdBy) {
        return addTaskInternal(title, description, priority, dueDate, kbId, action, actionPrompt, scheduledStart, createdBy);
    }

    private TaskItem addTaskInternal(String title, String description, String priority, String dueDate, Long kbId,
                                     String action, String actionPrompt, String scheduledStart, Long createdBy) {
        TaskItem task = new TaskItem();
        task.title = title;
        task.description = (description != null && !description.isEmpty()) ? description : null;
        task.priority = (priority != null && !priority.isEmpty()) ? priority : "mid";
        task.status = "pending";
        String now = java.time.LocalDate.now().toString();
        task.createdAt = now;
        task.updatedAt = now;
        task.dueDate = (dueDate != null && !dueDate.isEmpty()) ? dueDate : null;
        task.action = (action != null && !action.isEmpty()) ? action : null;
        task.actionPrompt = (actionPrompt != null && !actionPrompt.isEmpty()) ? actionPrompt : null;
        task.scheduledStart = (scheduledStart != null && !scheduledStart.isEmpty()) ? scheduledStart : null;
        task.createdBy = createdBy;

        insertTaskToDb(task, kbId);
        activityLogDbService.addLog("create_task", "创建任务: " + title, "user", null, null);
        return task;
    }

    public TaskItem updateTask(Long id, String title, String description, String status,
                                String priority, String dueDate) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, null, null, null, null, null);
    }

    public TaskItem updateTask(Long id, String title, String description, String status,
                                String priority, String dueDate, Long kbId) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, kbId, null, null, null, null);
    }

    public TaskItem updateTask(Long id, String title, String description, String status,
                                String priority, String dueDate, Long kbId,
                                String action, String actionPrompt) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, kbId, action, actionPrompt, null, null);
    }

    public TaskItem updateTask(Long id, String title, String description, String status,
                                String priority, String dueDate, Long kbId,
                                String action, String actionPrompt, String scheduledStart) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, kbId, action, actionPrompt, scheduledStart, null);
    }

    private TaskItem updateTaskInternal(Long id, String title, String description, String status,
                                String priority, String dueDate, Long kbId,
                                String action, String actionPrompt, String scheduledStart, String lastReminded) {
        TaskItem task = getTaskFromDb(id);
        if (task == null) return null;

        String oldStatus = task.status;

        if (title != null) task.title = title;
        if (description != null) task.description = description.isEmpty() ? null : description;
        if (status != null) task.status = status;
        if (priority != null) task.priority = priority;
        if (dueDate != null) task.dueDate = dueDate.isEmpty() ? null : dueDate;
        if (action != null) task.action = action.isEmpty() ? null : action;
        if (actionPrompt != null) task.actionPrompt = actionPrompt.isEmpty() ? null : actionPrompt;
        if (scheduledStart != null) task.scheduledStart = scheduledStart.isEmpty() ? null : scheduledStart;
        if (lastReminded != null) task.lastReminded = lastReminded;
        task.updatedAt = java.time.LocalDate.now().toString();

        boolean justDone = "done".equals(task.status) && !"done".equals(oldStatus);

        updateTaskInDb(task);
        activityLogDbService.addLog("update_task", "更新任务: " + task.title + " (状态: " + task.status + ")", "user", null, null);

        if (justDone) {
            sedimentCompletion(task);
        }
        return task;
    }

    public boolean markTaskReminded(Long id, String date) {
        TaskItem task = getTaskFromDb(id);
        if (task == null) return false;
        task.lastReminded = date;
        updateTaskInDb(task);
        return true;
    }

    /**
     * 任务完成自动沉淀 — 写入「任务完成记录」数据集，供日报/周报自动引用。
     */
    private void sedimentCompletion(TaskItem task) {
        try {
            cn.qihang.ai.assistant.datacenter.model.DataSet ds = dataSetService.getDataset(DONE_DATASET_NAME);
            if (ds == null) {
                ds = createDoneDataset();
            }
            if (ds == null) {
                log.warn("[任务] 自动创建数据集失败，跳过沉淀: {}", task.title);
                return;
            }
            Map<String, Object> record = new java.util.LinkedHashMap<>();
            record.put("任务标题", task.title);
            record.put("任务描述", task.description != null ? task.description : "");
            record.put("优先级", task.priority != null ? task.priority : "mid");
            record.put("完成日期", java.time.LocalDate.now().toString());
            int count = dataSetService.addRecords(ds.getId(), List.of(record), "task_done");
            log.info("[任务] 完成沉淀: {} → 数据集「{}」, 新增={}", task.title, DONE_DATASET_NAME, count);
        } catch (Exception e) {
            log.warn("[任务] 完成沉淀失败: {}", e.getMessage());
        }
    }

    private cn.qihang.ai.assistant.datacenter.model.DataSet createDoneDataset() {
        cn.qihang.ai.assistant.datacenter.model.DataSet ds = new cn.qihang.ai.assistant.datacenter.model.DataSet();
        ds.setName(DONE_DATASET_NAME);
        ds.setDescription("任务中心自动沉淀的任务完成记录，供日报/周报引用");
        ds.setType("tracking");
        ds.setStatus("active");
        cn.qihang.ai.assistant.datacenter.model.DataSchema schema = new cn.qihang.ai.assistant.datacenter.model.DataSchema();
        schema.setFields(List.of(
                field("任务标题", "text"),
                field("任务描述", "text"),
                field("优先级", "text"),
                field("完成日期", "date")
        ));
        ds.setSchema(schema);
        return dataSetService.createDataset(ds);
    }

    private cn.qihang.ai.assistant.datacenter.model.DataField field(String name, String type) {
        cn.qihang.ai.assistant.datacenter.model.DataField f = new cn.qihang.ai.assistant.datacenter.model.DataField();
        f.setName(name);
        f.setType(type);
        return f;
    }

    public boolean deleteTask(Long id) {
        return deleteTaskInternal(id, null);
    }

    public boolean deleteTask(Long id, Long kbId) {
        return deleteTaskInternal(id, kbId);
    }

    private boolean deleteTaskInternal(Long id, Long kbId) {
        deleteTaskFromDb(id);
        activityLogDbService.addLog("delete_task", "删除任务: " + id, "user", null, null);
        return true;
    }


    private List<TaskItem> getTasksByKbId(Long kbId) {
        List<TaskItem> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM tasks WHERE kb_id = ? ORDER BY created_at DESC")) {
            if (kbId != null) {
                ps.setLong(1, kbId);
            } else {
                ps.setObject(1, null);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            log.error("[任务] 按KB查询失败", e);
        }
        return tasks;
    }

    // ========== 任务执行记录 ==========

    public cn.qihang.ai.assistant.model.TaskData.TaskExecution createExecution(
            String executionId, Long taskId, String taskTitle, String triggerType, String triggeredBy) {
        String now = TimeUtil.nowStr();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO task_executions (execution_id, task_id, task_title, status, trigger_type, triggered_by, created_at) VALUES (?, ?, ?, 'QUEUED', ?, ?, ?)")) {
            ps.setString(1, executionId);
            ps.setLong(2, taskId);
            ps.setString(3, taskTitle);
            ps.setString(4, triggerType);
            ps.setString(5, triggeredBy);
            ps.setString(6, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 创建执行记录失败", e);
        }
        cn.qihang.ai.assistant.model.TaskData.TaskExecution ex = new cn.qihang.ai.assistant.model.TaskData.TaskExecution();
        ex.executionId = executionId;
        ex.taskId = taskId;
        ex.taskTitle = taskTitle;
        ex.status = "QUEUED";
        ex.triggerType = triggerType;
        ex.triggeredBy = triggeredBy;
        ex.createdAt = now;
        return ex;
    }

    public void startExecution(String executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE task_executions SET status='RUNNING', start_time=? WHERE execution_id=?")) {
            ps.setString(1, TimeUtil.nowStr());
            ps.setString(2, executionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 开始执行记录更新失败", e);
        }
    }

    public void appendExecutionLog(String executionId, String line) {
        try (Connection conn = dataSource.getConnection()) {
            String existing = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT log_text FROM task_executions WHERE execution_id = ?")) {
                ps.setString(1, executionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    existing = rs.getString(1);
                }
            }
            String newLog = (existing == null || existing.isBlank())
                    ? line
                    : existing + "\n" + line;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE task_executions SET log_text=? WHERE execution_id=?")) {
                ps.setString(1, newLog);
                ps.setString(2, executionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("[任务] 追加执行日志失败", e);
        }
    }

    public void completeExecution(String executionId, String resultText) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE task_executions SET status='SUCCESS', end_time=?, result_text=? WHERE execution_id=?")) {
            ps.setString(1, TimeUtil.nowStr());
            ps.setString(2, resultText);
            ps.setString(3, executionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 完成执行记录更新失败", e);
        }
    }

    public void failExecution(String executionId, String errorMessage) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE task_executions SET status='FAILED', end_time=?, error_message=? WHERE execution_id=?")) {
            ps.setString(1, TimeUtil.nowStr());
            ps.setString(2, errorMessage);
            ps.setString(3, executionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 失败执行记录更新失败", e);
        }
    }

    public void cancelExecution(String executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE task_executions SET status='CANCELLED', end_time=? WHERE execution_id=?")) {
            ps.setString(1, TimeUtil.nowStr());
            ps.setString(2, executionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 取消执行记录更新失败", e);
        }
    }

    public boolean hasActiveExecution(Long taskId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM task_executions WHERE task_id = ? AND status IN ('QUEUED', 'RUNNING')")) {
            ps.setLong(1, taskId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("[任务] 查询执行状态失败", e);
        }
        return false;
    }

    public List<cn.qihang.ai.assistant.model.TaskData.TaskExecution> getTaskExecutions(Long taskId) {
        List<cn.qihang.ai.assistant.model.TaskData.TaskExecution> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM task_executions WHERE task_id = ? ORDER BY id DESC")) {
            ps.setLong(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cn.qihang.ai.assistant.model.TaskData.TaskExecution ex = new cn.qihang.ai.assistant.model.TaskData.TaskExecution();
                ex.executionId = rs.getString("execution_id");
                ex.taskId = rs.getLong("task_id");
                ex.taskTitle = rs.getString("task_title");
                ex.status = rs.getString("status");
                ex.triggerType = rs.getString("trigger_type");
                ex.triggeredBy = rs.getString("triggered_by");
                ex.startTime = rs.getString("start_time");
                ex.endTime = rs.getString("end_time");
                ex.logText = rs.getString("log_text");
                ex.resultText = rs.getString("result_text");
                ex.errorMessage = rs.getString("error_message");
                ex.createdAt = rs.getString("created_at");
                list.add(ex);
            }
        } catch (SQLException e) {
            log.error("[任务] 查询执行记录失败", e);
        }
        return list;
    }

    public List<cn.qihang.ai.assistant.model.TaskData.TaskExecution> getAllExecutions() {
        List<cn.qihang.ai.assistant.model.TaskData.TaskExecution> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM task_executions ORDER BY id DESC LIMIT 200");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cn.qihang.ai.assistant.model.TaskData.TaskExecution ex = new cn.qihang.ai.assistant.model.TaskData.TaskExecution();
                ex.executionId = rs.getString("execution_id");
                ex.taskId = rs.getLong("task_id");
                ex.taskTitle = rs.getString("task_title");
                ex.status = rs.getString("status");
                ex.triggerType = rs.getString("trigger_type");
                ex.triggeredBy = rs.getString("triggered_by");
                ex.startTime = rs.getString("start_time");
                ex.endTime = rs.getString("end_time");
                ex.logText = rs.getString("log_text");
                ex.resultText = rs.getString("result_text");
                ex.errorMessage = rs.getString("error_message");
                ex.createdAt = rs.getString("created_at");
                list.add(ex);
            }
        } catch (SQLException e) {
            log.error("[任务] 查询全部执行记录失败", e);
        }
        return list;
    }
}
