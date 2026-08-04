package cn.qihang.ai.assistant.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static String YYYY = "yyyy";
    public static String YYYY_MM = "yyyy-MM";
    public static String YYYY_MM_DD = "yyyy-MM-dd";
    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

    public static LocalDateTime getNowDate() { return LocalDateTime.now(); }
    public static String getCurrentDateTime() { return LocalDateTime.now().format(DEFAULT_FORMATTER); }
    public static String getDate() { return dateTimeNow(YYYY_MM_DD); }
    public static final String getTime() { return dateTimeNow(YYYY_MM_DD_HH_MM_SS); }
    public static final String dateTimeNow() { return dateTimeNow(YYYYMMDDHHMMSS); }
    public static final String dateTimeNow(final String format) { return parseDateToStr(format, LocalDateTime.now()); }
    public static final String dateTime(final LocalDateTime date) { return parseDateToStr(YYYY_MM_DD, date); }
    public static final String parseDateToStr(final String format, final LocalDateTime date) {
        return DateTimeFormatter.ofPattern(format).format(date);
    }
    public static final LocalDateTime dateTime(final String format, final String ts) {
        return LocalDateTime.parse(ts, DateTimeFormatter.ofPattern(format));
    }
    public static LocalDateTime stringtoDate(String dateStr, String format) {
        try { return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(format)); } catch (Exception e) { return null; }
    }
    public static LocalDateTime stringtoDate(String dateStr) {
        try { return LocalDateTime.parse(dateStr, DEFAULT_FORMATTER); } catch (Exception e) { return null; }
    }
    public static final long dateTimeStrToTimeStamp(String format, final String time) {
        try {
            if (format == null || format.isEmpty()) format = YYYY_MM_DD_HH_MM_SS;
            LocalDateTime ldt = LocalDateTime.parse(time, DateTimeFormatter.ofPattern(format));
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) { return 0; }
    }
    public static final String datePath() {
        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }
    public static final String dateTimePath() {
        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    public static int differentDays(LocalDateTime date1, LocalDateTime date2) {
        return (int) Math.abs(Duration.between(date1, date2).toDays());
    }
    public static String timeDistance(LocalDateTime endTime, LocalDateTime startTime) {
        Duration duration = Duration.between(startTime, endTime);
        return duration.toDays() + "天" + duration.toHoursPart() + "小时" + duration.toMinutesPart() + "分钟";
    }
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DEFAULT_FORMATTER);
    }
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
}