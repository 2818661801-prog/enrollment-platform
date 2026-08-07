package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.request.ApplicationSubmitRequest;
import com.enroll.server.dto.request.ApplicationUpdateRequest;
import com.enroll.server.dto.request.WithdrawRequest;
import com.enroll.server.security.JwtUtil;
import com.enroll.server.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
 *   GET    /api/applications/my        — 我的报名（JWT 认证，手机号自动提取）
 *   GET    /api/applications/me        — 我的报名（JWT 认证，手机号自动提取）
 *   GET    /api/applications/check     — 报名查重
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JwtUtil jwtUtil;

    public ApplicationController(ApplicationService applicationService, JwtUtil jwtUtil) {
        this.applicationService = applicationService;
        this.jwtUtil = jwtUtil;
    }

    /** 提交报名 */
    @PostMapping
    public Map<String, Object> submit(@RequestBody @Valid ApplicationSubmitRequest req) {
        // Request DTO → Map 转换（ApplicationService.submit 仍接受 Map，后续可改）
        java.util.Map<String, Object> form = new java.util.HashMap<>();
        form.put("name", req.getName());
        form.put("idCard", req.getIdCard());
        form.put("phone", req.getPhone());
        form.put("gender", req.getGender());
        form.put("hasPhysics", req.getHasPhysics());
        form.put("hasEnglish", req.getHasEnglish());
        form.put("appliedCategory", req.getAppliedCategory());
        form.put("classId", req.getClassId());
        form.put("noticeAgreed", req.getNoticeAgreed());
        return R.ok("提交成功", applicationService.submit(form));
    }

    /**
     * 我的报名（JWT 认证，手机号从 token 自动提取，不再用 idCard 参数）
     * ⚠️ S2 修复：原 /my?idCard=xxx 任何人知道身份证就能查 → 改为必须带 JWT
     */
    @GetMapping("/my")
    public Map<String, Object> myApplications(HttpServletRequest request) {
        String phone = jwtUtil.getStudentPhoneFromAuthHeader(request.getHeader("Authorization"));
        if (phone == null) return R.fail(ResultCode.PARAM_INVALID, "请先登录");
        return R.ok(applicationService.findMyByPhone(phone));
    }

    /** 撤回报名（软删除：status → 0）
     *  2026-07-02 重构：PUT → POST（主人规则） */
    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody @Valid WithdrawRequest req) {
        applicationService.withdraw(req.getId());
        return R.ok("已撤回", null);
    }

    /** 修改报名信息（姓名/电话/选科）
     *  2026-07-02 重构：PUT → POST（主人规则） */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody @Valid ApplicationUpdateRequest req) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        if (req.getName() != null) body.put("name", req.getName());
        if (req.getPhone() != null) body.put("phone", req.getPhone());
        if (req.getIdCard() != null) body.put("idCard", req.getIdCard());
        if (req.getGender() != null) body.put("gender", req.getGender());
        if (req.getHasPhysics() != null) body.put("hasPhysics", req.getHasPhysics());
        if (req.getHasEnglish() != null) body.put("hasEnglish", req.getHasEnglish());
        applicationService.updateApp(req.getId(), body);
        return R.ok("修改成功", null);
    }

    /**
     * 我的报名（JWT 认证，手机号从 token 自动提取）
     * 路径 /me 不在拦截器范围内（拦截器只拦 /admin/**），
     * 所以手动从 Authorization header 解析 JWT。
     */
    @GetMapping("/me")
    public Map<String, Object> myApplicationsMe(HttpServletRequest request) {
        String phone = jwtUtil.getStudentPhoneFromAuthHeader(request.getHeader("Authorization"));
        if (phone == null) return R.fail(ResultCode.PARAM_INVALID, "缺少有效的登录凭证，请重新登录");
        return R.ok(applicationService.findMyByPhone(phone));
    }

    /**
     * 报名查重接口（GET，查询不写数据库）
     * 前端表单页输入手机号+身份证后失焦调用
     * GET /api/applications/check?phone=&idCard=&classId=
     */
    @GetMapping("/check")
    public Map<String, Object> checkDuplicate(
            @RequestParam String phone,
            @RequestParam String idCard,
            @RequestParam Integer classId) {
        if (phone == null || idCard == null || classId == null) {
            return R.fail(ResultCode.PARAM_INVALID, "参数不完整");
        }
        return R.ok(applicationService.checkDuplicate(phone, idCard, classId));
    }
}
