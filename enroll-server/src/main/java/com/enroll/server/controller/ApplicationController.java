package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.security.JwtUtil;
import com.enroll.server.service.ApplicationService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 报名 API 控制器（瘦控制器）
 *
 * RESTful：
 *   POST   /api/applications           — 提交报名
 *   GET    /api/applications/my        — 我的报名（idCard 参数）
 *   POST   /api/applications/my-verify — 密码查询
 *   GET    /api/applications/me        — 我的报名（JWT 认证，手机号自动提取）
 *   PUT    /api/applications/{id}/withdraw — 撤回报名（软删除）
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final ApplicationService applicationService;
    private final JwtUtil jwtUtil;

    public ApplicationController(ApplicationService applicationService, JwtUtil jwtUtil) {
        this.applicationService = applicationService;
        this.jwtUtil = jwtUtil;
    }

    /** 提交报名 */
    @PostMapping
    public Map<String, Object> submit(@RequestBody Map<String, Object> form) {
        return R.ok("提交成功", applicationService.submit(form));
    }

    /** 我的报名（仅返回已报名记录） */
    @GetMapping("/my")
    public Map<String, Object> myApplications(@RequestParam String idCard) {
        return R.ok(applicationService.findMy(idCard));
    }

    /** 验证查询密码后查询报名（身份证+密码双因子，POST body 防 URL 暴露密码） */
    @PostMapping("/my-verify")
    public Map<String, Object> myApplicationsWithPwd(@RequestBody Map<String, String> body) {
        return R.ok(applicationService.findMyWithPwd(body.get("idCard"), body.get("password")));
    }

    /** 撤回报名（软删除：status → 0） */
    @PutMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable Integer id) {
        applicationService.withdraw(id);
        return R.ok("已撤回", null);
    }

    /** 修改报名信息（姓名/电话/选科） */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        applicationService.updateApp(id, body);
        return R.ok("修改成功", null);
    }

    /**
     * 我的报名（JWT 认证，手机号从 token 自动提取）
     * 路径 /me 不在拦截器范围内（拦截器只拦 /admin/**），
     * 所以手动从 Authorization header 解析 JWT。
     */
    @GetMapping("/me")
    public Map<String, Object> myApplicationsMe(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return R.fail(ResultCode.PARAM_INVALID, "缺少有效的登录凭证，请重新登录");
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            String phone = claims.getSubject();
            String role = String.valueOf(claims.get("role"));
            if (!"student".equals(role)) {
                return R.fail(ResultCode.PARAM_INVALID, "无效的登录凭证");
            }
            log.info("【我的报名】phone={}", phone);
            return R.ok(applicationService.findMyByPhone(phone));
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return R.fail(ResultCode.PARAM_INVALID, "登录已过期，请重新登录");
        }
    }
}
