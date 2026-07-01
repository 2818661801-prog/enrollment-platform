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
 * 管理员认证控制器
 *
 * RESTful：
 *   POST /api/auth/login/send-code — 发送验证码（手机号）
 *   POST /api/auth/login/verify    — 验证验证码 → 返 JWT
 *
 * 验证码：6位数字，5分钟有效，存内存 Map
 *
 * 注意：此接口不在 /api/admin/** 下，不走 JWT 拦截器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 发送验证码
     */
    @PostMapping("/login/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.length() != 11) {
            return R.fail(ResultCode.PARAM_INVALID, "手机号格式不正确");
        }
        authService.sendCode(phone);
        return R.ok("验证码已发送（请查看服务器控制台）", null);
    }

    /**
     * 验证验证码 → 返 JWT
     */
    @PostMapping("/login/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");

        if (phone == null || code == null) {
            return R.fail(ResultCode.PARAM_INVALID, "手机号和验证码不能为空");
        }

        // 验证码校验
        if (!authService.verifyCode(phone, code)) {
            return R.fail(ResultCode.PARAM_INVALID, "验证码错误或已过期");
        }

        // 生成管理员 JWT（role=admin）
        String token = jwtUtil.generateAdmin(phone);
        log.info("管理员登录成功：phone={}", phone);
        return R.ok("登录成功", Map.of("token", token, "phone", phone));
    }
}
