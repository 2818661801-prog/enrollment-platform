package com.enroll.server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类（统一解析/格式化报名时间段）
 *
 * 现实例子：前端传的时间字符串可能是 "2026/09/01" 或 "2026/09/01 08:00"，
 * 后端拿到后必须统一解析成 LocalDateTime 才能和 MySQL 的 NOW() 比较。
 * 本类把所有可能的格式都试一遍，试到能解析为止。
 */
public final class DateUtils {

    private DateUtils() {}

    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final DateTimeFormatter ISO_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析日期时间字符串，支持多种格式
     * @param s 日期字符串
     * @param isStart true=纯日期补00:00，false=纯日期补23:59:59
     */
    public static LocalDateTime parseDateTime(String s, boolean isStart) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        // 尝试 yyyy/MM/dd HH:mm
        try { return LocalDateTime.parse(s, DATETIME_FMT); } catch (Exception ignored) {}
        // 尝试 yyyy-MM-dd HH:mm:ss
        try { return LocalDateTime.parse(s, ISO_DATETIME_FMT); } catch (Exception ignored) {}
        // 尝试 yyyy/MM/dd
        try {
            LocalDate d = LocalDate.parse(s, DATE_FMT);
            return isStart ? d.atStartOfDay() : d.atTime(23, 59, 59);
        } catch (Exception ignored) {}
        // 尝试 yyyy-MM-dd
        try {
            LocalDate d = LocalDate.parse(s, ISO_DATE_FMT);
            return isStart ? d.atStartOfDay() : d.atTime(23, 59, 59);
        } catch (Exception ignored) {}
        return null;
    }
}
