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
 * 2026-07-02 重构：所有"改数据库"接口统一改为 POST（主人规则：动数据库=POST，查数据库=GET）
 *
 * 接口列表：
 *   POST   /api/applications           — 提交报名
 *   POST   /api/applications/update    — 修改报名信息
 *   POST   /api/applications/withdraw  — 撤回报名
 *   GET    /api/applications/my        — 我的报名（idCard 参数）
 *   GET    /api/applications/me        — 我的报名（JWT 认证，手机号自动提取）
 *   POST   /api/applications/my-verify — 密码查询
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

    /** 撤回报名（软删除：status → 0）
     *  2026-07-02 重构：PUT → POST（主人规则） */
    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        if (id == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id 不能为空");
        }
        applicationService.withdraw(id);
        return R.ok("已撤回", null);
    }

    /** 修改报名信息（姓名/电话/选科）
     *  2026-07-02 重构：PUT → POST（主人规则） */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        if (id == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id 不能为空");
        }
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
