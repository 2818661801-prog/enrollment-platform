package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报名 API 控制器（瘦控制器）
 *
 * RESTful：
 *   POST   /api/applications         — 提交报名
 *   GET    /api/applications/my     — 我的报名（idCard 参数）
 *   PUT    /api/applications/{id}/withdraw — 撤回报名（软删除）
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** 提交报名 */
    @PostMapping
    public Map<String, Object> submit(@RequestBody Map<String, Object> form) {
        return R.ok("提交成功", applicationService.submit(form));
    }

    /** 我的报名 */
    @GetMapping("/my")
    public Map<String, Object> myApplications(@RequestParam String idCard) {
        return R.ok(applicationService.findMy(idCard));
    }

    /** 撤回报名（软删除：status → 0） */
    @PutMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable Integer id) {
        applicationService.withdraw(id);
        return R.ok("已撤回", null);
    }
}
