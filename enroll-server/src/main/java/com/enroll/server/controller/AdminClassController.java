package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.dto.request.*;
import com.enroll.server.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 管理端 · 班级相关 API（/api/admin/classes 等，从 AdminController 拆出）
 *
 * ⚠️ 2026-07-02 重构原则：改数据库只接受 POST，查数据库只接受 GET
 */
@RestController
@RequestMapping("/api/admin")
public class AdminClassController {

    private final ClassService classService;

    public AdminClassController(ClassService classService) {
        this.classService = classService;
    }

    /** 班级列表（含已删除，供管理后台用） */
    @GetMapping("/classes")
    public Map<String, Object> listClasses() {
        return R.ok(classService.listAllForAdmin());
    }

    /** 新建班级 */
    @PostMapping("/classes")
    public Map<String, Object> createClass(@RequestBody @Valid ClassCreateRequest req) {
        // ClassCreateRequest → Map 转换（ClassService.createClass 仍接受 Map，后续可改）
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", req.getName());
        body.put("quota", req.getQuota());
        body.put("description", req.getDescription());
        body.put("classRounds", req.getClassRounds());
        body.put("categoryNames", req.getCategoryNames());
        return R.ok("创建成功", classService.createClass(body));
    }

    /** 更新班级（含软删除、round、quota 等所有字段） */
    @PostMapping("/classes/update")
    public Map<String, Object> updateClass(@RequestBody @Valid ClassUpdateRequest req) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", req.getId());
        if (req.getName() != null) body.put("name", req.getName());
        if (req.getQuota() != null) body.put("quota", req.getQuota());
        if (req.getDescription() != null) body.put("description", req.getDescription());
        if (req.getIsDeleted() != null) body.put("isDeleted", req.getIsDeleted());
        if (req.getGroupInfo() != null) body.put("groupInfo", req.getGroupInfo());
        if (req.getClassRounds() != null) body.put("classRounds", req.getClassRounds());
        if (req.getCategoryNames() != null) body.put("categoryNames", req.getCategoryNames());
        return R.ok("更新成功", classService.updateClass(req.getId(), body));
    }

    /** 修改报名时间段 */
    @PostMapping("/classes/update-period")
    public Map<String, Object> updatePeriod(@RequestBody @Valid PeriodUpdateRequest req) {
        // 保留原 AdminController 的空值校验（行为不能回归）
        if (req.getPeriod() == null || req.getPeriod().isBlank()) {
            return R.fail(ResultCode.PARAM_INVALID, "period 不能为空");
        }
        return R.ok("修改成功", classService.updatePeriod(req.getId(), req.getPeriod()));
    }

    /** 修改配额 */
    @PostMapping("/classes/update-quota")
    public Map<String, Object> updateQuota(@RequestBody @Valid QuotaUpdateRequest req) {
        return R.ok("修改成功", classService.updateQuota(req.getId(), req.getQuota()));
    }

    /** 软删除班级 */
    @PostMapping("/classes/delete")
    public Map<String, Object> deleteClass(@RequestBody @Valid IdRequest req) {
        classService.deleteClass(req.getId());
        return R.ok("已删除", null);
    }

    /** 获取全部班级-类别关联（给低代码平台同步用） */
    @GetMapping("/classCategory")
    public Map<String, Object> listClassCategories() {
        return R.ok(classService.listClassCategories());
    }

    /** 新增班级-类别关联（class_category 中间表） */
    @PostMapping("/classCategory")
    public Map<String, Object> addClassCategory(@RequestBody @Valid ClassCategoryRequest req) {
        return classService.addClassCategory(req.getClassId(), req.getCategoryId());
    }

    /** 删除班级-类别关联（按主键 id） */
    @PostMapping("/classCategory/delete")
    public Map<String, Object> deleteClassCategory(@RequestBody @Valid ClassCategoryDeleteRequest req) {
        return classService.deleteClassCategory(req.getId());
    }
}
