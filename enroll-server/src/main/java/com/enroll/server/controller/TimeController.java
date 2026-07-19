package com.enroll.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器时间接口（无需鉴权，学生端前端用来校正本地时间）
 *
 * GET /api/time — 返回服务器当前时间戳（毫秒）
 *
 * 为什么后端直接提供时间：
 * ① 服务器在大陆，时区即北京时间，不需要第三方 API
 * ② 前端浏览器时间可被用户篡改，用服务器时间做基准更可靠
 * ③ 前端计算"本地时间-服务器时间"偏差，后续判断都用 trustedNow()
 *
 * ⚠️ S15 修复：同一 IP 每秒最多 1 次（内存 ConcurrentHashMap，不依赖 Redis）
 */
@RestController
@RequestMapping("/api")
public class TimeController {

    // S15 修复：内存 Map 限流，key=IP，value=上次请求时间戳
    private static final ConcurrentHashMap<String, Long> RATE_LIMIT = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 1000; // 1秒窗口

    @GetMapping("/time")
    public Map<String, Object> getServerTime(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (ip == null) ip = "unknown";
        long now = System.currentTimeMillis();

        Long last = RATE_LIMIT.get(ip);
        if (last != null && now - last < WINDOW_MS) {
            // 返回限流响应（注意 Map.of 不允许 null value，改用 HashMap）
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("code", 429);
            resp.put("message", "请求过于频繁");
            resp.put("data", null);
            return resp;
        }
        RATE_LIMIT.put(ip, now);

        return Map.of("serverTime", now);
    }

    @GetMapping("/year")
    public Map<String, Object> getServerYear() {
        int year = java.time.LocalDate.now().getYear();
        return Map.of("year", year);
    }

    /** 清理过期 IP（防止内存泄漏，惰性清理） */
    public static void cleanExpired() {
        long now = System.currentTimeMillis();
        RATE_LIMIT.forEach((ip, ts) -> {
            if (now - ts > WINDOW_MS * 2) RATE_LIMIT.remove(ip);
        });
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
