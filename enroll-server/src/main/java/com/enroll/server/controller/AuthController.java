package com.enroll.server.controller;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.R;
import com.enroll.server.security.JwtUtil;
import com.enroll.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 *
 * 管理员（账号密码）：
 *   POST /api/auth/login  — {username, password} → JWT
 *
 * 学生（手机验证码）：
 *   POST /api/auth/send-code  — {phone} → 发送验证码
 *   POST /api/auth/login/sms  — {phone, code} → JWT
 *
 * 注意：此接口不在 /api/admin/** 下，不走管理员 JWT 拦截器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    // 固定管理员账号
    private static final String ADMIN_USER = "***REMOVED***";
    private static final String ADMIN_PWD  = "***REMOVED***";

    public AuthController(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    /** 管理员账号密码登录 → 返 JWT */
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

    /** 学生发送验证码（Redis 存储，5分钟有效） */
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        authService.sendCode(phone);
        return R.ok("验证码已发送", null);
    }

    /** 学生验证码登录 → 返 JWT */
    @PostMapping("/login/sms")
    public Map<String, Object> loginSms(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code  = body.get("code");
        String token = authService.verifyCodeAndLogin(phone, code);
        return R.ok("登录成功", Map.of("token", token, "phone", phone));
    }
}