package com.enroll.server.controller;

import com.enroll.server.dto.ApplicationDTO;
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
 *   GET    /api/admin/config/{key}              — 读 sys_config
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
 *     POST /api/admin/config/set                 — 改 sys_config 通用配置
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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
        log.info("批量录取: ids={}, auditComment={}", idList, auditComment);
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
        log.info("批量未录取: ids={}, auditComment={}", idList, auditComment);
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
        log.info("批量删除报名: ids={}", idList);
        return R.ok("已删除 " + idList.size() + " 条记录", null);
    }

    /**
     * 清空某班所有报名记录（软删除）
     * Body: { classId: 1 }
     */
    @Transactional
    @PostMapping("/applications/clear")
    public Map<String, Object> clearClass(@RequestBody Map<String, Object> body) {
        Integer classId = (Integer) body.get("classId");
        if (classId == null) {
            return R.fail(ResultCode.PARAM_INVALID, "classId 不能为空");
        }
        applicationService.clearClass(classId);
        log.info("清空班级报名: classId={}", classId);
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
        Integer id = (Integer) body.get("id");
        if (id == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id 不能为空");
        }
        log.info("=== UPDATE CLASS id={} body={}", id, body);
        return R.ok("更新成功", classService.updateClass(id, body));
    }

    /** 修改报名时间段 */
    @Transactional
    @PostMapping("/classes/update-period")
    public Map<String, Object> updatePeriod(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        String period = (String) body.get("period");
        if (id == null || period == null || period.isBlank()) {
            return R.fail(ResultCode.PARAM_INVALID, "id / period 不能为空");
        }
        return R.ok("修改成功", classService.updatePeriod(id, period));
    }

    /** 修改配额 */
    @Transactional
    @PostMapping("/classes/update-quota")
    public Map<String, Object> updateQuota(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        Integer quota = (Integer) body.get("quota");
        if (id == null || quota == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id / quota 不能为空");
        }
        return R.ok("修改成功", classService.updateQuota(id, quota));
    }

    /** 软删除班级 */
    @Transactional
    @PostMapping("/classes/delete")
    public Map<String, Object> deleteClass(@RequestBody Map<String, Object> body) {
        Integer id = (Integer) body.get("id");
        if (id == null) {
            return R.fail(ResultCode.PARAM_INVALID, "id 不能为空");
        }
        log.info("=== DELETE CLASS id={}", id);
        classService.deleteClass(id);
        return R.ok("已删除", null);
    }

    // ==================== 报名须知 / sys_config · 读 ====================

    /**
     * 读取报名须知（2026-07-02 新增显式 API）
     * 返回 {title, conditions[], notices[]} 结构
     */
    @GetMapping("/notice")
    public Map<String, Object> getNotice() {
        return sysConfigRepo.findByCfgKey("notice")
                .map(cfg -> R.ok(parseJsonOrEmpty(cfg.getCfgValue())))
                .orElse(R.ok(Map.of("title", "", "conditions", List.of(), "notices", List.of())));
    }

    /** 读取 sys_config（通用配置） */
    @GetMapping("/config/{key}")
    public Map<String, Object> getConfig(@PathVariable String key) {
        return sysConfigRepo.findByCfgKey(key)
                .map(cfg -> R.ok(cfg.getCfgValue()))
                .orElse(R.fail(ResultCode.PARAM_INVALID, "配置项不存在: " + key));
    }

    // ==================== 报名须知 / sys_config · 写（全部 POST） ====================

    /**
     * 更新报名须知（2026-07-02 新增显式 API）
     * Body: { title: "...", conditions: [...], notices: [...] }
     * 内部存到 sys_config.cfgKey='notice'.cfgValue（JSON 字符串）
     */
    @Transactional
    @PostMapping("/notice/update")
    public Map<String, Object> updateNotice(@RequestBody Map<String, Object> body) {
        String cfgValue;
        try {
            cfgValue = objectMapper.writeValueAsString(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("报名须知 JSON 序列化失败", ex);
            return R.fail(ResultCode.PARAM_INVALID, "body 序列化失败: " + ex.getMessage());
        }
        saveConfig("notice", cfgValue, (String) body.getOrDefault("updatedBy", "admin"));
        log.info("更新报名须知: {}", body);
        return R.ok("保存成功", null);
    }

    /**
     * 更新 sys_config（通用配置）
     * Body: { key: "xxx", cfgValue: "...", updatedBy: "admin" }
     */
    @Transactional
    @PostMapping("/config/set")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        String key = (String) body.get("key");
        String cfgValue = (String) body.get("cfgValue");
        String updatedBy = (String) body.getOrDefault("updatedBy", "admin");
        if (key == null || cfgValue == null) {
            return R.fail(ResultCode.PARAM_INVALID, "key / cfgValue 不能为空");
        }
        saveConfig(key, cfgValue, updatedBy);
        log.info("更新配置: key={}, updatedBy={}", key, updatedBy);
        return R.ok("保存成功", null);
    }

    /** 通用 saveConfig 逻辑（避免重复） */
    private void saveConfig(String key, String cfgValue, String updatedBy) {
        SysConfig cfg = sysConfigRepo.findByCfgKey(key)
                .orElseGet(() -> {
                    SysConfig newCfg = new SysConfig();
                    newCfg.setCfgKey(key);
                    return newCfg;
                });
        cfg.setCfgValue(cfgValue);
        cfg.setUpdatedBy(updatedBy);
        sysConfigRepo.save(cfg);
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

    /** 解析 JSON 字符串为 Map，空值兜底为 {} */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonOrEmpty(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            log.warn("JSON 解析失败: {}", json);
            return Map.of();
        }
    }

    // ObjectMapper 静态注入（Spring 推荐构造注入，这里因为用 @PostMapping 较多，提取为静态方便）
    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
}