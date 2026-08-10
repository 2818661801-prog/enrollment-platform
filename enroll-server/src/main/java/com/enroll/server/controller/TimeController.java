package com.enroll.server.controller;

import com.enroll.server.util.IpUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器时间接口（无需鉴权，学生端前端用来校正本地时间）
 *
 * GET /api/time — 返回服务器当前时间戳（毫秒），用 MySQL NOW() 保证与报名截止判断同一时钟源
 *
 * ⚠️ 为什么用 MySQL NOW() 而不是 System.currentTimeMillis()：
 * 后端 findCurrentRound 用 MySQL NOW() 判断报名截止，如果 /api/time 用应用服务器时钟，
 * 两台机器差 1 分钟就会导致前端显示的截止时间和后端实际判断差 1 分钟。
 *
 * ⚠️ S15 修复：同一 IP 每秒最多 1 次（内存 ConcurrentHashMap，不依赖 Redis）
 */
@RestController
@RequestMapping("/api")
public class TimeController {

    @PersistenceContext
    private EntityManager entityManager;

    // S15 修复：内存 Map 限流，key=IP，value=上次请求时间戳
    private static final ConcurrentHashMap<String, Long> RATE_LIMIT = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 1000; // 1秒窗口

    @GetMapping("/time")
    public Map<String, Object> getServerTime(HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        if (ip == null) ip = "unknown";
        long now = System.currentTimeMillis();

        Long last = RATE_LIMIT.get(ip);
        if (last != null && now - last < WINDOW_MS) {
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("code", 429);
            resp.put("message", "请求过于频繁");
            resp.put("data", null);
            return resp;
        }
        RATE_LIMIT.put(ip, now);

        // 用 MySQL NOW() 获取时间戳，保证与报名截止判断（findCurrentRound）同一时钟源
        BigDecimal mysqlNowMs = (BigDecimal) entityManager
                .createNativeQuery("SELECT UNIX_TIMESTAMP(NOW(3)) * 1000")
                .getSingleResult();
        return Map.of("serverTime", mysqlNowMs.longValue());
    }

    @GetMapping("/year")
    public Map<String, Object> getServerYear() {
        int year = java.time.LocalDate.now().getYear();
        return Map.of("year", year);
    }

    /** 每 60 秒清理过期 IP，防止内存泄漏（原来是死代码，从未被调用） */
    @Scheduled(fixedRate = 60_000)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        RATE_LIMIT.forEach((ip, ts) -> {
            if (now - ts > WINDOW_MS * 2) RATE_LIMIT.remove(ip);
        });
    }

}
