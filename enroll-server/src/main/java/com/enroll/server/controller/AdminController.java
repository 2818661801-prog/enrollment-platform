package com.enroll.server.controller;

import com.enroll.server.dto.ClassDTO;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.R;
import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.SysConfigRepository;
import com.enroll.server.service.ApplicationService;
import com.enroll.server.service.ClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 API（/api/admin/** 全部需要 JWT 鉴权）
 *
 * 接口列表：
 *
 * 报名管理：
 *   GET    /api/admin/applications                  — 分页查询（classId/status/idCard/name）
 *   DELETE /api/admin/applications/batch           — 批量删除（ids=1,2,3）
 *   DELETE /api/admin/applications/clear           — 清空某班所有报名（classId）
 *   PUT    /api/admin/applications/admit/batch     — 批量录取（ids=1,2,3）
 *
 * 班级管理：
 *   GET    /api/admin/classes                      — 班级列表（含已删除）
 *   POST   /api/admin/classes                      — 新增班级
 *   PUT    /api/admin/classes/{id}                — 更新班级（含软删除）
 *   PUT    /api/admin/classes/{id}/period         — 修改时间段
 *   PUT    /api/admin/classes/{id}/quota          — 修改配额
 *   DELETE /api/admin/classes/{id}                — 软删除班级
 *
 * 系统配置：
 *   GET    /api/admin/config/{key}                 — 读取配置
 *   PUT    /api/admin/config/{key}                 — 更新配置
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ClassService classService;
    private final ApplicationService applicationService;
    private final SysConfigRepository sysConfigRepo;

    public AdminController(ClassService classService,
                           ApplicationService applicationService,
                           SysConfigRepository sysConfigRepo) {
        this.classService = classService;
        this.applicationService = applicationService;
        this.sysConfigRepo = sysConfigRepo;
    }

    // ==================== 报名管理 ====================

    /**
     * 分页查询报名记录
     * @param page     页码（从0开始）
     * @param size     每页条数
     * @param classId  按班级ID筛选（可选）
     * @param status   按状态筛选（可选，1已报/0撤回/2录取）
     * @param idCard   身份证号模糊搜索（可选）
     * @param name     姓名模糊搜索（可选）
     */
    @GetMapping("/applications")
    public Map<String, Object> listApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String name) {
        Page<com.enroll.server.dto.ApplicationDTO> result =
                applicationService.adminSearch(classId, status, idCard, name, PageRequest.of(page, size));
        return R.ok(Map.of(
                "list", result.getContent(),
                "total", result.getTotalElements(),
                "pages", result.getTotalPages()
        ));
    }

    /**
     * 批量删除报名记录（软删除：status→0）
     */
    @Transactional
    @DeleteMapping("/applications/batch")
    public Map<String, Object> batchDelete(@RequestParam String ids) {
        List<Integer> idList = parseIds(ids);
        applicationService.batchUpdateStatus(idList, ApplicationService.STATUS_WITHDRAWN);
        log.info("批量删除报名: ids={}", idList);
        return R.ok("已删除 " + idList.size() + " 条记录", null);
    }

    /**
     * 清空某班所有报名记录（软删除）
     */
    @Transactional
    @DeleteMapping("/applications/clear")
    public Map<String, Object> clearClass(@RequestParam Integer classId) {
        applicationService.clearClass(classId);
        log.info("清空班级报名: classId={}", classId);
        return R.ok("已清空该班所有报名记录", null);
    }

    /**
     * 批量录取（is_admitted=1）
     */
    @Transactional
    @PutMapping("/applications/admit/batch")
    public Map<String, Object> batchAdmit(@RequestParam String ids) {
        List<Integer> idList = parseIds(ids);
        applicationService.batchAdmit(idList);
        log.info("批量录取: ids={}", idList);
        return R.ok("已录取 " + idList.size() + " 名学生", null);
    }

    // ==================== 班级管理 ====================

    /** 班级列表（含已删除，供管理后台用） */
    @GetMapping("/classes")
    public Map<String, Object> listClasses() {
        return R.ok(classService.listAllForAdmin());
    }

    /** 新增班级 */
    @Transactional
    @PostMapping("/classes")
    public Map<String, Object> createClass(@RequestBody Map<String, Object> body) {
        return R.ok("创建成功", classService.createClass(body));
    }

    /** 更新班级（含软删除、round、quota 等所有字段） */
    @Transactional
    @PutMapping("/classes/{id}")
    public Map<String, Object> updateClass(@PathVariable Integer id,
                                            @RequestBody Map<String, Object> body) {
        return R.ok("更新成功", classService.updateClass(id, body));
    }

    /** 修改报名时间段 */
    @Transactional
    @PutMapping("/classes/{id}/period")
    public Map<String, Object> updatePeriod(@PathVariable Integer id,
                                             @RequestBody Map<String, String> body) {
        String period = body.get("period");
        if (period == null || period.isBlank()) {
            return R.fail(ResultCode.PARAM_INVALID, "时间段不能为空");
        }
        return R.ok("修改成功", classService.updatePeriod(id, period));
    }

    /** 修改配额 */
    @Transactional
    @PutMapping("/classes/{id}/quota")
    public Map<String, Object> updateQuota(@PathVariable Integer id,
                                             @RequestBody Map<String, Integer> body) {
        Integer quota = body.get("quota");
        if (quota == null) {
            return R.fail(ResultCode.PARAM_INVALID, "配额不能为空");
        }
        return R.ok("修改成功", classService.updateQuota(id, quota));
    }

    /** 软删除班级 */
    @Transactional
    @DeleteMapping("/classes/{id}")
    public Map<String, Object> deleteClass(@PathVariable Integer id) {
        classService.deleteClass(id);
        return R.ok("已删除", null);
    }

    // ==================== 系统配置 ====================

    /**
     * 读取配置
     * @param key cfg_key（notice / admin_phone）
     */
    @GetMapping("/config/{key}")
    public Map<String, Object> getConfig(@PathVariable String key) {
        return sysConfigRepo.findByCfgKey(key)
                .map(cfg -> R.ok(cfg.getCfgValue()))
                .orElse(R.fail(ResultCode.PARAM_INVALID, "配置项不存在: " + key));
    }

    /**
     * 更新配置
     * @param key   cfg_key
     * @param body  {cfgValue, updatedBy}
     */
    @Transactional
    @PutMapping("/config/{key}")
    public Map<String, Object> updateConfig(@PathVariable String key,
                                              @RequestBody Map<String, String> body) {
        SysConfig cfg = sysConfigRepo.findByCfgKey(key)
                .orElseGet(() -> {
                    SysConfig newCfg = new SysConfig();
                    newCfg.setCfgKey(key);
                    return newCfg;
                });
        cfg.setCfgValue(body.get("cfgValue"));
        cfg.setUpdatedBy(body.get("updatedBy"));
        sysConfigRepo.save(cfg);
        log.info("更新配置: key={}, updatedBy={}", key, body.get("updatedBy"));
        return R.ok("保存成功", null);
    }

    // ==================== 内部工具 ====================

    /** 逗号分隔 ID 字符串 → List<Integer> */
    private List<Integer> parseIds(String ids) {
        return java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(java.util.stream.Collectors.toList());
    }
}
