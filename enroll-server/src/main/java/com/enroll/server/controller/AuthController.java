package com.enroll.server.controller;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.R;
import com.enroll.server.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员认证控制器
 *
 * 账号密码登录（固定账号，无需数据库）：
 *   POST /api/auth/login  — {username, password} → JWT
 *
 * 注意：此接口不在 /api/admin/** 下，不走 JWT 拦截器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtUtil jwtUtil;

    // 固定管理员账号（后续可扩展为数据库存储）
    private static final String ADMIN_USER = "***REMOVED***";
    private static final String ADMIN_PWD  = "***REMOVED***";

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 账号密码登录 → 返 JWT
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return R.fail(ResultCode.PARAM_INVALID, "账号和密码不能为空");
        }

        if (!ADMIN_USER.equals(username) || !ADMIN_PWD.equals(password)) {
            return R.fail(ResultCode.PARAM_INVALID, "账号或密码错误");
        }

        String token = jwtUtil.generateAdmin(username);
        log.info("管理员登录成功：username={}", username);
        return R.ok("登录成功", Map.of("token", token, "username", username));
    }
}