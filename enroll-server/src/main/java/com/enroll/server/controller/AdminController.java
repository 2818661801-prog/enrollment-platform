package com.enroll.server.controller;

import com.enroll.server.dto.ApplicationDTO;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.R;
import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.SysConfigRepository;
import com.enroll.server.service.ApplicationService;
import com.enroll.server.service.ClassService;
import com.enroll.server.util.RequestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 API（/api/admin/** 全部需要 JWT 鉴权）
 *
 * ⚠️ 2026-07-02 重构原则：所有"改数据库"的接口只接受 POST，"查数据库"只接受 GET
 *    （主人 2026-07-02 明确：内网低代码平台调本项目 API 只走 POST/GET，更安全）
 *    历史 PUT/DELETE 接口已全部重构为 POST，路径加 /update /delete /admit 等动作后缀
 *
 * 接口列表（最新）：
 *
 * 读（GET）：
 *   GET    /api/admin/applications              — 分页查询（classId/status/idCard/name）
 *   GET    /api/admin/applications/{id}         — 单条详情（2026-07-02 新增）
 *   GET    /api/admin/classes                   — 班级列表（含已删除）
 *   GET    /api/admin/categories                — 类别列表
 *   GET    /api/admin/notice                    — 报名须知
 *
 * 写（POST）：
 *   报名管理：
 *     POST /api/admin/applications/admit         — 批量录取
 *     POST /api/admin/applications/reject        — 批量未录取
 *     POST /api/admin/applications/delete        — 批量软删
 *     POST /api/admin/applications/clear         — 清空某班所有报名
 *   班级管理：
 *     POST /api/admin/classes                    — 新建班级
 *     POST /api/admin/classes/update             — 更新班级
 *     POST /api/admin/classes/update-period      — 仅改时间段
 *     POST /api/admin/classes/update-quota       — 仅改配额
 *     POST /api/admin/classes/delete             — 软删除班级
 *   类别管理：见 AdminCategoryController.java（GET/POST/PUT/DELETE 全套）
 *   系统配置：
 *     POST /api/admin/notice/update              — 改报名须知
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ClassService classService;
    private final ApplicationService applicationService;
    private final SysConfigRepository sysConfigRepo;
    private final com.enroll.server.repository.ApplicationRepository applicationRepo;

    public AdminController(ClassService classService,
                           ApplicationService applicationService,
                           SysConfigRepository sysConfigRepo,
                           com.enroll.server.repository.ApplicationRepository applicationRepo) {
        this.classService = classService;
        this.applicationService = applicationService;
        this.sysConfigRepo = sysConfigRepo;
        this.applicationRepo = applicationRepo;
    }

    // ==================== 报名管理 · 读 ====================

    /**
     * 分页查询报名记录
     * @param page     页码（从0开始）
     * @param size     每页条数
     * @param classId  按班级ID筛选（可选）
     * @param status   按状态筛选（可选，0已报/1撤回/2录取/3未录取）
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
        // 分页边界保护：page < 0 → 0，size <= 0 → 20，size > 100 → 100
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Page<ApplicationDTO> result =
                applicationService.adminSearch(classId, status, idCard, name, PageRequest.of(safePage, safeSize));
        return R.ok(Map.of(
                "list", result.getContent(),
                "total", result.getTotalElements(),
                "pages", result.getTotalPages()
        ));
    }

    /**
     * 报名记录详情（2026-07-02 新增，内网审批流程高频需求）
     */
    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable Integer id) {
        return applicationRepo.findById(id)
                .map(app -> {
                    // 查班级名（className 来自 classes 表，DTO 需要这个字段）
                    String className = classService.listAllForAdmin().stream()
                            .filter(c -> c.getId().equals(app.getClassId()))
                            .map(c -> c.getName())
                            .findFirst().orElse(null);
                    return R.ok(applicationService.toDTO(app, className));
                })
                .orElse(R.fail(ResultCode.PARAM_INVALID, "报名记录不存在: id=" + id));
    }

    // ==================== 报名管理 · 写（全部 POST） ====================

    /**
     * 批量录取（status=2）
     * Body: { ids: [1,2,3] 或 "1,2,3", auditComment: "..." }
     */
    @Transactional
    @PostMapping("/applications/admit")
    public Map<String, Object> batchAdmit(@RequestBody Map<String, Object> body) {
        List<Integer> idList = parseIdsField(body.get("ids"));
        String auditComment = (String) body.getOrDefault("auditComment", "");
        applicationService.batchAdmit(idList, auditComment);
        // log.info("批量录取: ids={}, auditComment={}", idList, auditComment);
        return R.ok("已录取 " + idList.size() + " 名学生", null);
    }

    /**
     * 批量未录取（status=3）
     * Body: { ids: [1,2,3] 或 "1,2,3", auditComment: "..." }
     */
    @Transactional
    @PostMapping("/applications/reject")
    public Map<String, Object> batchReject(@RequestBody Map<String, Object> body) {
        List<Integer> idList = parseIdsField(body.get("ids"));
        String auditComment = (String) body.getOrDefault("auditComment", "");
        applicationService.batchReject(idList, auditComment);
        // log.info("批量未录取: ids={}, auditComment={}", idList, auditComment);
        return R.ok("已设置 " + idList.size() + " 名学生为未录取", null);
    }

    /**
     * 批量删除报名记录（软删除：status→0）
     * Body: { ids: [1,2,3] 或 "1,2,3" }
     */
    @Transactional
    @PostMapping("/applications/delete")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body) {
        List<Integer> idList = parseIdsField(body.get("ids"));
        applicationService.batchUpdateStatus(idList, ApplicationService.STATUS_WITHDRAWN);
        // log.info("批量删除报名: ids={}", idList);
        return R.ok("已删除 " + idList.size() + " 条记录", null);
    }

    /**
     * 清空某班所有报名记录（软删除）
     * Body: { classId: 1 }
     */
    @Transactional
    @PostMapping("/applications/clear")
    public Map<String, Object> clearClass(@RequestBody Map<String, Object> body) {
        Integer classId = RequestUtils.parseId(body, "classId");
        applicationService.clearClass(classId);
        // log.info("清空班级报名: classId={}", classId);
        return R.ok("已清空该班所有报名记录", null);
    }

    // ==================== 班级管理 · 读 ====================

    /** 班级列表（含已删除，供管理后台用） */
    @GetMapping("/classes")
    public Map<String, Object> listClasses() {
        return R.ok(classService.listAllForAdmin());
    }

    // ==================== 班级管理 · 写（全部 POST） ====================

    /** 新建班级 */
    @Transactional
    @PostMapping("/classes")
    public Map<String, Object> createClass(@RequestBody Map<String, Object> body) {
        return R.ok("创建成功", classService.createClass(body));
    }

    /** 更新班级（含软删除、round、quota 等所有字段） */
    @Transactional
    @PostMapping("/classes/update")
    public Map<String, Object> updateClass(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        // log.info("=== UPDATE CLASS id={} body={}", id, body);
        return R.ok("更新成功", classService.updateClass(id, body));
    }

    /** 修改报名时间段 */
    @Transactional
    @PostMapping("/classes/update-period")
    public Map<String, Object> updatePeriod(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        String period = (String) body.get("period");
        if (period == null || period.isBlank()) {
            return R.fail(ResultCode.PARAM_INVALID, "period 不能为空");
        }
        return R.ok("修改成功", classService.updatePeriod(id, period));
    }

    /** 修改配额 */
    @Transactional
    @PostMapping("/classes/update-quota")
    public Map<String, Object> updateQuota(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        Integer quota = RequestUtils.parseId(body, "quota");
        return R.ok("修改成功", classService.updateQuota(id, quota));
    }

    /** 软删除班级 */
    @Transactional
    @PostMapping("/classes/delete")
    public Map<String, Object> deleteClass(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        // log.info("=== DELETE CLASS id={}", id);
        classService.deleteClass(id);
        return R.ok("已删除", null);
    }

    // ==================== 报名须知 / sys_config · 读 ====================

    /**
     * 读取报名须知
     * 返回 {title, conditions, notices} 结构（换行分隔字符串）
     */
    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return sysConfigRepo.findById(1)
                .map(cfg -> R.ok(java.util.Map.of(
                    "title", cfg.getTitle() != null ? cfg.getTitle() : "",
                    "conditions", cfg.getConditions() != null ? cfg.getConditions() : "",
                    "notices", cfg.getNotices() != null ? cfg.getNotices() : ""
                )))
                .orElse(R.ok(java.util.Map.of("title", "", "conditions", "", "notices", "")));
    }

    // ==================== 报名须知 · 写（全部 POST） ====================

    /**
     * 更新报名须知
     * Body: { title: "...", conditions: "条件1\n条件2", notices: "须知1\n须知2", updatedBy: "admin" }
     */
    @Transactional
    @PostMapping("/notice/update")
    public Map<String, Object> updateNotice(@RequestBody Map<String, Object> body) {
        SysConfig cfg = sysConfigRepo.findById(1).orElse(new SysConfig());
        cfg.setTitle((String) body.get("title"));
        cfg.setConditions((String) body.get("conditions"));
        cfg.setNotices((String) body.get("notices"));
        sysConfigRepo.save(cfg);
        // log.info("更新报名须知: title={}", body.get("title"));
        return R.ok("保存成功", null);
    }

    // ==================== 内部工具 ====================

    /** 解析 ids 字段：支持 [1,2,3] 或 "1,2,3" */
    private List<Integer> parseIdsField(Object idsObj) {
        if (idsObj == null) return List.of();
        if (idsObj instanceof List) {
            return ((List<?>) idsObj).stream()
                    .map(i -> ((Number) i).intValue())
                    .toList();
        }
        return java.util.Arrays.stream(String.valueOf(idsObj).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(java.util.stream.Collectors.toList());
    }

}