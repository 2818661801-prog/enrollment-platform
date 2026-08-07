package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.NoticeUpdateRequest;
import com.enroll.server.service.SysConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 管理端 · 报名须知 API（/api/admin/notice 等，从 AdminController 拆出）
 *
 * ⚠️ 2026-07-02 重构原则：改数据库只接受 POST，查数据库只接受 GET
 */
@RestController
@RequestMapping("/api/admin")
public class AdminNoticeController {

    private final SysConfigService sysConfigService;

    public AdminNoticeController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /** 读取报名须知 */
    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return R.ok(sysConfigService.getNotice());
    }

    /** 更新报名须知 */
    @PostMapping("/notice/update")
    public Map<String, Object> updateNotice(@RequestBody @Valid NoticeUpdateRequest req) {
        sysConfigService.updateNotice(
            req.getTitle(), req.getConditions(), req.getNotices(),
            req.getContactInfo(), req.getUpdatedBy());
        return R.ok("保存成功", null);
    }
}
