package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.*;
import com.enroll.server.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 管理端 · 报名记录 API（/api/admin/applications 等，从 AdminController 拆出）
 *
 * ⚠️ 2026-07-02 重构原则：改数据库只接受 POST，查数据库只接受 GET
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApplicationController {

    private final ApplicationService applicationService;

    public AdminApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** 报名记录全量查询（不分页，给低代码平台同步用） */
    @GetMapping("/applications/all")
    public Map<String, Object> listAllApplications() {
        return R.ok(applicationService.findAllForSync());
    }

    /** 报名记录详情（含 idCardRaw，管理员可信角色可见） */
    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable Integer id) {
        return applicationService.findByIdForAdmin(id);
    }

    /** 批量录取（status=2） */
    @PostMapping("/applications/admit")
    public Map<String, Object> batchAdmit(@RequestBody @Valid BatchAdmitRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要录取的记录", null);
        applicationService.batchAdmit(req.getIds(), req.getAuditComment());
        return R.ok("已录取 " + req.getIds().size() + " 名学生", null);
    }

    /** 批量未录取（status=3） */
    @PostMapping("/applications/reject")
    public Map<String, Object> batchReject(@RequestBody @Valid BatchRejectRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要驳回的记录", null);
        applicationService.batchReject(req.getIds(), req.getAuditComment());
        return R.ok("已设置 " + req.getIds().size() + " 名学生为未录取", null);
    }

    /** 批量删除报名记录（软删除：status→0） */
    @PostMapping("/applications/delete")
    public Map<String, Object> batchDelete(@RequestBody @Valid BatchDeleteRequest req) {
        if (req.getIds().isEmpty()) return R.ok("没有需要删除的记录", null);
        applicationService.batchUpdateStatus(req.getIds(), ApplicationService.STATUS_WITHDRAWN);
        return R.ok("已删除 " + req.getIds().size() + " 条记录", null);
    }

    /** 清空某班所有报名记录（软删除） */
    @PostMapping("/applications/clear")
    public Map<String, Object> clearClass(@RequestBody @Valid ClearClassRequest req) {
        applicationService.clearClass(req.getClassId());
        return R.ok("已清空该班所有报名记录", null);
    }
}
