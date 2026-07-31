package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.model.TaskData.*;
import cn.qihang.ai.assistant.service.db.ActivityLogDbService;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
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
                     "INSERT INTO tasks (id, title, description, status, priority, due_date, created_at, updated_at, kb_id, action, action_prompt, last_reminded) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id=VALUES(id), title=VALUES(title), description=VALUES(description), status=VALUES(status), priority=VALUES(priority), due_date=VALUES(due_date), created_at=VALUES(created_at), updated_at=VALUES(updated_at), kb_id=VALUES(kb_id), action=VALUES(action), action_prompt=VALUES(action_prompt), last_reminded=VALUES(last_reminded)")) {
            ps.setString(1, task.id);
            ps.setString(2, task.title);
            ps.setString(3, task.description);
            ps.setString(4, task.status);
            ps.setString(5, task.priority);
            ps.setString(6, task.dueDate);
            ps.setString(7, task.createdAt);
            ps.setString(8, task.updatedAt);
            if (kbId != null) {
                ps.setLong(9, kbId);
            } else {
                ps.setNull(9, Types.INTEGER);
            }
            ps.setString(10, task.action);
            ps.setString(11, task.actionPrompt);
            ps.setString(12, task.lastReminded);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 插入失败", e);
        }
    }

    private TaskItem mapRowToTask(ResultSet rs) throws SQLException {
        TaskItem task = new TaskItem();
        task.id = rs.getString("id");
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
        long kbId = rs.getLong("kb_id");
        task.kbId = rs.wasNull() ? null : kbId;
        return task;
    }

    private TaskItem getTaskFromDb(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            ps.setString(1, id);
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
                     "UPDATE tasks SET title=?, description=?, status=?, priority=?, due_date=?, updated_at=?, action=?, action_prompt=?, last_reminded=? WHERE id=?")) {
            ps.setString(1, task.title);
            ps.setString(2, task.description);
            ps.setString(3, task.status);
            ps.setString(4, task.priority);
            ps.setString(5, task.dueDate);
            ps.setString(6, task.updatedAt);
            ps.setString(7, task.action);
            ps.setString(8, task.actionPrompt);
            ps.setString(9, task.lastReminded);
            ps.setString(10, task.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 更新失败", e);
        }
    }

    private void deleteTaskFromDb(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[任务] 删除失败", e);
        }
    }

    // ========== Public API ==========

    public List<TaskItem> getAllTasks() {
        return getAllTasksFromDb();
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate) {
        return addTaskInternal(title, description, priority, dueDate, null, null, null);
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate, Long kbId) {
        return addTaskInternal(title, description, priority, dueDate, kbId, null, null);
    }

    public TaskItem addTask(String title, String description, String priority, String dueDate, Long kbId,
                            String action, String actionPrompt) {
        return addTaskInternal(title, description, priority, dueDate, kbId, action, actionPrompt);
    }

    private TaskItem addTaskInternal(String title, String description, String priority, String dueDate, Long kbId,
                                     String action, String actionPrompt) {
        TaskItem task = new TaskItem();
        task.id = "T" + System.currentTimeMillis();
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

        insertTaskToDb(task, kbId);
        activityLogDbService.addLog("create_task", "创建任务: " + title, "user", null, null);
        return task;
    }

    public TaskItem updateTask(String id, String title, String description, String status,
                                String priority, String dueDate) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, null, null, null, null);
    }

    public TaskItem updateTask(String id, String title, String description, String status,
                                String priority, String dueDate, Long kbId) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, kbId, null, null, null);
    }

    public TaskItem updateTask(String id, String title, String description, String status,
                                String priority, String dueDate, Long kbId,
                                String action, String actionPrompt) {
        return updateTaskInternal(id, title, description, status, priority, dueDate, kbId, action, actionPrompt, null);
    }

    private TaskItem updateTaskInternal(String id, String title, String description, String status,
                                String priority, String dueDate, Long kbId,
                                String action, String actionPrompt, String lastReminded) {
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

    public boolean markTaskReminded(String id, String date) {
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

    public boolean deleteTask(String id) {
        return deleteTaskInternal(id, null);
    }

    public boolean deleteTask(String id, Long kbId) {
        return deleteTaskInternal(id, kbId);
    }

    private boolean deleteTaskInternal(String id, Long kbId) {
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
}
