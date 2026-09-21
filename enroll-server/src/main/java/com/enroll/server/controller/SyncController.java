package com.enroll.server.controller;

import com.enroll.server.dto.request.SyncApplicationsRequest;
import com.enroll.server.dto.request.SyncCategoriesRequest;
import com.enroll.server.dto.request.SyncClassesRequest;
import com.enroll.server.dto.request.SyncConfigRequest;
import com.enroll.server.service.ApplicationService;
import com.enroll.server.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 低代码平台数据同步 API（瘦身后：只收 Body → 调 SyncService，业务全在 Service）
 *
 * 同步按钮流程：
 *   ① b查a  GET /api/admin/classes  → 低代码平台了解 enroll_db 现状
 *   ② 在b里做增删改                  → 低代码平台操作自己的内网 DB
 *   ③ b查b                          → 低代码平台拿到 b 的完整数据
 *   ④ 增删a  POST /api/admin/sync/{table}  → 全量同步到 enroll_db
 */
@RestController
@RequestMapping("/api/admin/sync")
public class SyncController {

    private final SyncService syncService;
    private final ApplicationService applicationService;

    public SyncController(SyncService syncService, ApplicationService applicationService) {
        this.syncService = syncService;
        this.applicationService = applicationService;
    }

    /** 全量同步班级（先清再插） */
    @PostMapping("/classes")
    public Map<String, Object> syncClasses(@RequestBody @Valid SyncClassesRequest req) {
        return syncService.syncClasses(req.getData());
    }

    /** 全量同步类别（先清再插） */
    @PostMapping("/categories")
    public Map<String, Object> syncCategories(@RequestBody @Valid SyncCategoriesRequest req) {
        return syncService.syncCategories(req.getData());
    }

    /** 全量同步系统配置 */
    @PostMapping("/config")
    public Map<String, Object> syncConfig(@RequestBody @Valid SyncConfigRequest req) {
        return syncService.syncConfig(req.getData());
    }

    /**
     * 同步报名记录（低代码平台推送报名/录取/驳回状态）
     * 主键：idCard + classId；b有a无→新增、b有a有→更新、显式 isDeleted=1→软删
     * 业务逻辑在 ApplicationService.syncFromLowCode()（2026-09-21 从 v1 热修复移植）
     */
    @PostMapping("/applications")
    public Map<String, Object> syncApplications(@RequestBody @Valid SyncApplicationsRequest req) {
        return applicationService.syncFromLowCode(req.getData());
    }
}
