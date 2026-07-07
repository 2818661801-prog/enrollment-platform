package com.enroll.server.security;

import com.enroll.server.dto.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 *
 * 作用：所有 /api/admin/** 路径必须带合法 JWT
 *       /api/student/** 和 /api/classes/** 不拦截（学生匿名）
 *
 * 拦截流程：
 *   1. 取请求头 Authorization
 *   2. 验证格式："Bearer xxx"
 *   3. 解析 token → 把 subject(身份证) 放进 request attribute
 *   4. 失败 → 直接返回 401（不走 Controller）
 *
 * 配置开关：jwt.enabled = false 时，拦截器放行所有请求
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.enabled}")
    private Boolean enabled;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    private final JwtUtil jwtUtil;

    /** 构造器注入 */
    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 鉴权关闭时直接放行
        if (!enabled) {
            return true;
        }

        // S8 修复：优先从 Authorization header 拿，header 没有则从 httpOnly Cookie 拿
        String authHeader = request.getHeader(header);
        String token = null;

        if (authHeader != null && authHeader.startsWith(prefix)) {
            token = authHeader.substring(prefix.length());
        } else {
            // 从 httpOnly Cookie 读取（JS 无法伪造，因为 httpOnly）
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("admin_token".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }

        if (token == null || token.isBlank()) {
            writeJson(response, ResultCode.PARAM_INVALID.getCode(), "缺少登录凭证");
            return false;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            // 把用户信息放进 request，Controller 用 @RequestAttribute 拿
            request.setAttribute("currentUser", claims.getSubject());
            request.setAttribute("currentRole", claims.get("role"));
            return true;
        } catch (Exception e) {
            // log.warn("JWT 解析失败: {}", e.getMessage());
            writeJson(response, ResultCode.PARAM_INVALID.getCode(), "token 无效或已过期");
            return false;
        }
    }

    /** 写 JSON 错误响应 */
    private void writeJson(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", code, message)
        );
    }
}
