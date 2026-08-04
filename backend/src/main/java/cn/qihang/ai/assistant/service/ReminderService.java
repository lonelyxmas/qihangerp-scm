package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.model.ReminderData.Reminder;
import cn.qihang.ai.assistant.service.db.NotificationDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final ZoneId TZ = ZoneId.of("Asia/Shanghai");

    
    private final FeishuService feishuService;

    private final DataSource dataSource;

    private final NotificationDbService notificationDbService;

    public ReminderService(FeishuService feishuService, DataSource dataSource, NotificationDbService notificationDbService) {
        this.feishuService = feishuService;
        this.dataSource = dataSource;
        this.notificationDbService = notificationDbService;
    }

    // ========== SQLite CRUD ==========

    private List<Reminder> getAllRemindersFromDb() {
        List<Reminder> reminders = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM reminders ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reminders.add(mapRowToReminder(rs));
            }
        } catch (SQLException e) {
            log.error("[提醒] 查询失败", e);
        }
        return reminders;
    }

    private void insertReminderToDb(Reminder r, Long kbId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO reminders (name, message, type, time, date, day_of_week, day_of_month, month_day, enabled, created_at, last_triggered, kb_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.name);
            ps.setString(2, r.message);
            ps.setString(3, r.type);
            ps.setString(4, r.time);
            ps.setString(5, r.date);
            ps.setString(6, r.dayOfWeek);
            ps.setString(7, r.dayOfMonth);
            ps.setString(8, r.monthDay);
            ps.setInt(9, r.enabled ? 1 : 0);
            ps.setString(10, r.createdAt);
            ps.setString(11, r.lastTriggered);
            if (kbId != null) {
                ps.setLong(12, kbId);
            } else {
                ps.setNull(12, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.id = keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            log.error("[提醒] 插入失败", e);
        }
    }

    private Reminder mapRowToReminder(ResultSet rs) throws SQLException {
        Reminder r = new Reminder();
        r.id = rs.getLong("id");
        r.name = rs.getString("name");
        r.message = rs.getString("message");
        r.type = rs.getString("type");
        r.time = rs.getString("time");
        r.date = rs.getString("date");
        r.dayOfWeek = rs.getString("day_of_week");
        r.dayOfMonth = rs.getString("day_of_month");
        r.monthDay = rs.getString("month_day");
        r.enabled = rs.getInt("enabled") == 1;
        r.createdAt = rs.getString("created_at");
        r.lastTriggered = rs.getString("last_triggered");
        return r;
    }

    private Reminder getReminderFromDb(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM reminders WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToReminder(rs);
            }
        } catch (SQLException e) {
            log.error("[提醒] 查询失败", e);
        }
        return null;
    }

    private void updateReminderInDb(Reminder r) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE reminders SET name=?, message=?, type=?, time=?, date=?, day_of_week=?, day_of_month=?, month_day=?, enabled=?, last_triggered=? WHERE id=?")) {
            ps.setString(1, r.name);
            ps.setString(2, r.message);
            ps.setString(3, r.type);
            ps.setString(4, r.time);
            ps.setString(5, r.date);
            ps.setString(6, r.dayOfWeek);
            ps.setString(7, r.dayOfMonth);
            ps.setString(8, r.monthDay);
            ps.setInt(9, r.enabled ? 1 : 0);
            ps.setString(10, r.lastTriggered);
            ps.setLong(11, r.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[提醒] 更新失败", e);
        }
    }

    private void deleteReminderFromDb(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reminders WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("[提醒] 删除失败", e);
        }
    }

    // ========== Public API ==========

    public List<Reminder> getAllReminders() {
        return getAllRemindersFromDb();
    }

    public List<Reminder> getEnabledReminders() {
        return getAllRemindersFromDb().stream()
                .filter(r -> r.enabled)
                .collect(Collectors.toList());
    }

    public Reminder addReminder(String name, String message, String type, String time,
                                 String date, String dayOfWeek, String dayOfMonth, String monthDay) {
        return addReminderInternal(name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, null);
    }

    public Reminder addReminder(String name, String message, String type, String time,
                                 String date, String dayOfWeek, String dayOfMonth, String monthDay, Long kbId) {
        return addReminderInternal(name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, kbId);
    }

    private Reminder addReminderInternal(String name, String message, String type, String time,
                                 String date, String dayOfWeek, String dayOfMonth, String monthDay, Long kbId) {
        Reminder r = new Reminder();
        r.name = name;
        r.message = message;
        r.type = type;
        r.time = normalizeTime(time);

        if ("once".equals(type)) {
            r.date = (date != null && !date.isBlank()) ? date : LocalDate.now(TZ).toString();
        } else if ("weekly".equals(type)) {
            r.dayOfWeek = dayOfWeek;
        } else if ("monthly".equals(type)) {
            r.dayOfMonth = dayOfMonth;
        } else if ("yearly".equals(type)) {
            r.monthDay = monthDay;
        }
        r.enabled = true;
        r.createdAt = LocalDateTime.now(TZ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        insertReminderToDb(r, kbId);

        if (kbId != null) {
        }

        log.info("[提醒] 新增提醒: {} ({})", name, type);
        return r;
    }

    public boolean updateReminder(Long id, String name, String message, String type,
                                    String time, String date, String dayOfWeek, String dayOfMonth,
                                    String monthDay, Boolean enabled) {
        return updateReminderInternal(id, name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, enabled, null);
    }

    public boolean updateReminder(Long id, String name, String message, String type,
                                    String time, String date, String dayOfWeek, String dayOfMonth,
                                    String monthDay, Boolean enabled, Long kbId) {
        return updateReminderInternal(id, name, message, type, time, date, dayOfWeek, dayOfMonth, monthDay, enabled, kbId);
    }

    private boolean updateReminderInternal(Long id, String name, String message, String type,
                                    String time, String date, String dayOfWeek, String dayOfMonth,
                                    String monthDay, Boolean enabled, Long kbId) {
        Reminder r = getReminderFromDb(id);
        if (r == null) return false;

        if (name != null) r.name = name;
        if (message != null) r.message = message;
        if (type != null) r.type = type;
        if (time != null && !time.isBlank()) {
            String normalized = normalizeTime(time);
            if (!normalized.equals(r.time)) {
                r.time = normalized;
                r.lastTriggered = null;
            }
        }

        String reminderType = (type != null) ? type : r.type;
        if ("once".equals(reminderType)) {
            if (date != null) {
                String newDate = date.isBlank() ? LocalDate.now(TZ).toString() : date;
                if (!newDate.equals(r.date)) {
                    r.date = newDate;
                    r.lastTriggered = null;
                }
            }
            r.dayOfWeek = null;
            r.dayOfMonth = null;
            r.monthDay = null;
        } else if ("weekly".equals(reminderType)) {
            r.date = null;
            if (dayOfWeek != null) r.dayOfWeek = dayOfWeek;
            r.dayOfMonth = null;
            r.monthDay = null;
        } else if ("monthly".equals(reminderType)) {
            r.date = null;
            r.dayOfWeek = null;
            if (dayOfMonth != null) r.dayOfMonth = dayOfMonth;
            r.monthDay = null;
        } else if ("yearly".equals(reminderType)) {
            r.date = null;
            r.dayOfWeek = null;
            r.dayOfMonth = null;
            if (monthDay != null) r.monthDay = monthDay;
        } else {
            r.date = null;
            r.dayOfWeek = null;
            r.dayOfMonth = null;
            r.monthDay = null;
        }

        if (enabled != null) r.enabled = enabled;

        updateReminderInDb(r);

        if (kbId != null) {
        }

        log.info("[提醒] 更新提醒: {} ({})", r.name, type);
        return true;
    }

    private String normalizeTime(String time) {
        if (time == null || time.isBlank()) return "09:00";
        String[] parts = time.split(":");
        if (parts.length != 2) return "09:00";
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return String.format("%02d:%02d", hour, minute);
        } catch (NumberFormatException e) {
            return "09:00";
        }
    }

    public boolean deleteReminder(Long id) {
        return deleteReminderInternal(id, null);
    }

    public boolean deleteReminder(Long id, Long kbId) {
        return deleteReminderInternal(id, kbId);
    }

    private boolean deleteReminderInternal(Long id, Long kbId) {
        deleteReminderFromDb(id);

        if (kbId != null) {
        }

        log.info("[提醒] 删除提醒: {}", id);
        return true;
    }

    public boolean toggleReminder(Long id) {
        return toggleReminderInternal(id, null);
    }

    public boolean toggleReminder(Long id, Long kbId) {
        return toggleReminderInternal(id, kbId);
    }

    private boolean toggleReminderInternal(Long id, Long kbId) {
        Reminder r = getReminderFromDb(id);
        if (r == null) return false;

        r.enabled = !r.enabled;
        updateReminderInDb(r);

        if (kbId != null) {
        }

        log.info("[提醒] {}提醒: {}", r.enabled ? "启用" : "禁用", r.name);
        return true;
    }

    public void triggerReminder(Reminder r) {
        triggerReminder(r, null);
    }

    public void triggerReminder(String notesDir, Reminder r) {
        triggerReminder(r, null);
    }

    public void triggerReminder(Reminder r, Long kbId) {
        if (r == null || !r.enabled) return;

        // 先标记已触发，保证当天去重生效（无论推送成败，都不重复触发）
        try {
            r.lastTriggered = LocalDateTime.now(TZ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            updateReminderInDb(r);
        } catch (Exception e) {
            log.error("[提醒] 更新触发状态失败: {} - {}", r.name, e.getMessage());
        }

        try {
            String content = r.message != null && !r.message.isBlank() ? r.message : "该提醒了！";
            String scheduleText = getReminderDescription(r);

            // 站内通知（Web 页面弹出）
            try {
                notificationDbService.addNotification(
                        1L,
                        "⏰ " + r.name,
                        (scheduleText != null && !scheduleText.isBlank() ? "📅 " + scheduleText + "\n" : "") + content,
                        "reminder", "reminder", String.valueOf(r.id));
            } catch (Exception e) {
                log.warn("[提醒] 站内通知写入失败: {} - {}", r.name, e.getMessage());
            }

            // 飞书推送：⏰ 提醒 + 名称 + 分隔线，有备注才加备注
            List<List<Map<String, String>>> contentBlocks = new ArrayList<>();
            contentBlocks.add(List.of(Map.of("tag", "text", "text", "🔔 " + r.name)));
            contentBlocks.add(List.of(Map.of("tag", "text", "text", "━━━━━━━━━━━━━━━━━━")));
            if (r.message != null && !r.message.isBlank()) {
                contentBlocks.add(List.of(Map.of("tag", "text", "text", r.message)));
            }
            boolean success = feishuService.sendPost("⏰ 提醒", contentBlocks);
            if (!success) {
                log.warn("[提醒] 触发失败(飞书发送未成功): {}", r.name);
            }

            log.info("[提醒] 触发提醒: {}", r.name);
        } catch (Exception e) {
            log.error("[提醒] 触发失败: {} - {}", r.name, e.getMessage());
        }
    }

    public List<Reminder> getDueReminders() {
        List<Reminder> due = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(TZ);
        LocalTime currentTime = now.toLocalTime();
        String currentWeekday = String.valueOf(now.getDayOfWeek().getValue());
        String currentMonth = String.valueOf(now.getMonthValue());
        String currentDayOfMonth = String.valueOf(now.getDayOfMonth());

        for (Reminder r : getEnabledReminders()) {
            if (!shouldTriggerNow(r, now, currentTime, currentWeekday, currentMonth, currentDayOfMonth)) {
                continue;
            }
            if (wasTriggeredToday(r, now)) {
                log.info("[提醒] 今日已触发，跳过: {}", r.name);
                continue;
            }
            log.info("[提醒] 准备触发: {}", r.name);
            due.add(r);
        }
        return due;
    }

    private boolean shouldTriggerNow(Reminder r, ZonedDateTime now, LocalTime currentTime,
                                      String currentWeekday, String currentMonth, String currentDayOfMonth) {
        String[] parts = r.time.split(":");
        if (parts.length != 2) return false;

        int triggerHour = Integer.parseInt(parts[0]);
        int triggerMinute = Integer.parseInt(parts[1]);
        int currentHour = currentTime.getHour();
        int currentMinute = currentTime.getMinute();

        // 未到计划时间不触发；已到时间则触发（当天未触发过由 wasTriggeredToday 去重，
        // 可容忍调度延迟数分钟而不漏发）
        if (currentHour < triggerHour || (currentHour == triggerHour && currentMinute < triggerMinute)) {
            return false;
        }

        return switch (r.type) {
            case "daily" -> true;
            case "once" -> r.date != null && r.date.equals(now.toLocalDate().toString());
            case "weekly" -> r.dayOfWeek != null && r.dayOfWeek.equals(currentWeekday);
            case "monthly" -> r.dayOfMonth != null && r.dayOfMonth.equals(currentDayOfMonth);
            case "yearly" -> {
                if (r.monthDay == null) yield false;
                String[] md = r.monthDay.split("-");
                yield md.length == 2 &&
                        md[0].equals(currentMonth) &&
                        md[1].equals(currentDayOfMonth);
            }
            default -> false;
        };
    }

    private boolean wasTriggeredToday(Reminder r, ZonedDateTime now) {
        if (r.lastTriggered == null) return false;
        return r.lastTriggered.startsWith(now.toLocalDate().toString());
    }

    public String getTypeLabel(String type) {
        return switch (type) {
            case "daily" -> "每天";
            case "once" -> "一次";
            case "weekly" -> "每周";
            case "monthly" -> "每月";
            case "yearly" -> "每年";
            default -> type;
        };
    }

    public String getWeekdayLabel(String dayOfWeek) {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case "1" -> "周一";
            case "2" -> "周二";
            case "3" -> "周三";
            case "4" -> "周四";
            case "5" -> "周五";
            case "6" -> "周六";
            case "7" -> "周日";
            default -> dayOfWeek;
        };
    }

    public String getReminderDescription(Reminder r) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTypeLabel(r.type));
        if (r.time != null) {
            sb.append(" ").append(r.time);
        }
        if ("once".equals(r.type) && r.date != null) {
            sb.append(" (").append(r.date).append(")");
        } else if ("weekly".equals(r.type) && r.dayOfWeek != null) {
            sb.append(" (").append(getWeekdayLabel(r.dayOfWeek)).append(")");
        }
        if ("monthly".equals(r.type) && r.dayOfMonth != null) {
            sb.append(" (").append(r.dayOfMonth).append("号)");
        }
        if ("yearly".equals(r.type) && r.monthDay != null) {
            sb.append(" (").append(r.monthDay.replace("-", "月")).append("日)");
        }
        return sb.toString();
    }

    private List<Reminder> getRemindersByKbId(Long kbId) {
        List<Reminder> reminders = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM reminders WHERE kb_id = ? ORDER BY created_at DESC")) {
            if (kbId != null) {
                ps.setLong(1, kbId);
            } else {
                ps.setObject(1, null);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reminders.add(mapRowToReminder(rs));
            }
        } catch (SQLException e) {
            log.error("[提醒] 按KB查询失败", e);
        }
        return reminders;
    }
}
