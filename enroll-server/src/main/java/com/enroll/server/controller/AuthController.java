package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.request.AdminLoginRequest;
import com.enroll.server.dto.request.SmsLoginRequest;
import com.enroll.server.dto.request.SmsSendRequest;
import com.enroll.server.service.AuthService;
import com.enroll.server.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
 *   POST /api/auth/login/sms  — {phone, code} → JWT + 报名状态
 *
 * 注意：此接口不在 /api/admin/** 下，不走管理员 JWT 拦截器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    // 管理员账号密码（⚠️ 2026-08-07 fail-closed 整改：不再写死 fallback，
    // 改从 application.yml 读取 ${ADMIN_USERNAME:***REMOVED***} / ${ADMIN_PASSWORD:***REMOVED***}，
    // 开发环境 yml 有 fallback 方便调试，生产由堡垒机环境变量覆盖）
    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthController(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    /** 管理员账号密码登录 → 返 JWT（⚠️ S8 修复：同时设 httpOnly Cookie） */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid AdminLoginRequest req, HttpServletResponse response) {
        // @Valid + @NotBlank 已保证 username/password 非空，这里只需比对
        if (!adminUsername.equals(req.getUsername()) || !adminPassword.equals(req.getPassword())) {
            return R.fail(ResultCode.PARAM_INVALID, "账号或密码错误");
        }

        String token = jwtUtil.generateAdmin(req.getUsername());
        // S8 修复：httpOnly Cookie，JS 无法通过 document.cookie 读取
        Cookie cookie = new Cookie("admin_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24小时
        cookie.setSecure(true);                              // HTTPS 才传 Cookie
        cookie.setAttribute("SameSite", "Strict");           // 防 CSRF
        response.addCookie(cookie);

        return R.ok("登录成功", Map.of("token", token, "username", req.getUsername()));
    }

    /** 学生发送验证码（Redis 存储，5分钟有效） */
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody @Valid SmsSendRequest req) {
        authService.sendCode(req.getPhone());
        return R.ok("验证码已发送", null);
    }

    /**
     * 学生验证码登录 → 返 JWT + 报名状态（⚠️ S8 修复：同时设 httpOnly Cookie）
     * status=4 表示未报名（登录后无记录）
     */
    @PostMapping("/login/sms")
    public Map<String, Object> loginSms(@RequestBody @Valid SmsLoginRequest req, HttpServletResponse response) {
        String token = authService.verifyCodeAndLogin(req.getPhone(), req.getCode());

        // S8 修复：httpOnly Cookie，JS 无法通过 document.cookie 读取
        Cookie cookie = new Cookie("student_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24小时
        response.addCookie(cookie);

        // 查该手机号是否有有效报名记录（status=1/2/3/4）——逻辑下沉到 AuthService
        return authService.getLoginResponse(req.getPhone(), token);
    }
}
