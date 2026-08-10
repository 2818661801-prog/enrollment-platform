package com.enroll.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 提取工具（统一处理反向代理头）
 *
 * 28.5 抽取：TimeController 和 AuthService 各写了一份 getClientIp()，逻辑相同，
 * 统一到此处避免重复。AuthService 版本多了 "unknown" 检查和 try-catch，合并后保留更健壮的版本。
 */
public final class IpUtil {

    private IpUtil() {}

    /**
     * 从 HttpServletRequest 提取客户端真实 IP
     *
     * 优先级：X-Forwarded-For → X-Real-IP → RemoteAddr
     * 多级代理时取第一个 IP（逗号分隔）
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从 RequestContextHolder 提取客户端 IP（无需传 request 参数）
     *
     * 适用于 Service 层无法直接获取 HttpServletRequest 的场景（如 AuthService）
     * @return IP 地址，获取失败返回 "unknown"
     */
    public static String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            return getClientIp(attrs.getRequest());
        } catch (Exception e) {
            return "unknown";
        }
    }
}
