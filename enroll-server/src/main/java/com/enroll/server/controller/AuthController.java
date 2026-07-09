package com.enroll.server.controller;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.R;
import com.enroll.server.entity.Application;
import com.enroll.server.repository.ApplicationRepository;
import com.enroll.server.security.JwtUtil;
import com.enroll.server.service.AuthService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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
    private final ApplicationRepository appRepo;

    // 固定管理员账号（⚠️ S3 修复：生产通过环境变量注入，禁止写死）
    private static final String ADMIN_USER = System.getenv("AUTH_ADMIN_USER") != null
        ? System.getenv("AUTH_ADMIN_USER") : "***REMOVED***";
    private static final String ADMIN_PWD  = System.getenv("AUTH_ADMIN_PASSWORD") != null
        ? System.getenv("AUTH_ADMIN_PASSWORD") : "***REMOVED***";

    public AuthController(JwtUtil jwtUtil, AuthService authService, ApplicationRepository appRepo) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.appRepo = appRepo;
    }

    /** 管理员账号密码登录 → 返 JWT（⚠️ S8 修复：同时设 httpOnly Cookie） */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return R.fail(ResultCode.PARAM_INVALID, "账号和密码不能为空");
        }
        if (!ADMIN_USER.equals(username) || !ADMIN_PWD.equals(password)) {
            return R.fail(ResultCode.PARAM_INVALID, "账号或密码错误");
        }

        String token = jwtUtil.generateAdmin(username);
        // S8 修复：httpOnly Cookie，JS 无法通过 document.cookie 读取
        Cookie cookie = new Cookie("admin_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24小时
        cookie.setSecure(true);                              // HTTPS 才传 Cookie
        cookie.setAttribute("SameSite", "Strict");           // 防 CSRF
        response.addCookie(cookie);

        // log.info("管理员登录成功：username={}", username);
        return R.ok("登录成功", Map.of("token", token, "username", username));
    }

    /** 学生发送验证码（Redis 存储，5分钟有效） */
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        authService.sendCode(phone);
        return R.ok("验证码已发送", null);
    }

    /**
     * 学生验证码登录 → 返 JWT + 报名状态（⚠️ S8 修复：同时设 httpOnly Cookie）
     * status=4 表示未报名（登录后无记录）
     */
    @PostMapping("/login/sms")
    public Map<String, Object> loginSms(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String phone = body.get("phone");
        String code  = body.get("code");
        String token = authService.verifyCodeAndLogin(phone, code);

        // S8 修复：httpOnly Cookie，JS 无法通过 document.cookie 读取
        Cookie cookie = new Cookie("student_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/api");
        cookie.setMaxAge(86400); // 24小时
        response.addCookie(cookie);

        // 查该手机号是否有有效报名记录（status=1/2/3/4）
        List<Application> apps = appRepo.findByPhoneAndStatusIn(phone, List.of(1, 2, 3, 4));
        if (apps.isEmpty()) {
            // 无记录 → status=0，未报名
            return R.ok("登录成功", Map.of("token", token, "phone", phone,
                    "hasRegistration", false, "status", 0));
        }
        Application app = apps.get(0);
        return R.ok("登录成功", Map.of(
                "token", token,
                "phone", phone,
                "hasRegistration", true,
                "status", app.getStatus(),
                "classId", app.getClassId(),
                "applyTime", app.getApplyTime().toString()
        ));
    }
}