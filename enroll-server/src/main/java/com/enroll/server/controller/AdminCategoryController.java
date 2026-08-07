package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.dto.request.CategoryCreateRequest;
import com.enroll.server.dto.request.CategoryDeleteRequest;
import com.enroll.server.dto.request.CategoryUpdateRequest;
import com.enroll.server.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 管理端 - 类别字典管理
 *
 * ⚠️ 2026-07-02 重构：所有"改数据库"只接受 POST，"查"只接受 GET
 *    PUT /api/admin/categories/{id}  → POST /api/admin/categories/update
 *    DELETE /api/admin/categories/{id} → POST /api/admin/categories/delete
 *
 * 接口列表：
 *   GET    /api/admin/categories            — 列表
 *   POST   /api/admin/categories            — 新增
 *   POST   /api/admin/categories/update     — 修改（Body 传 id）
 *   POST   /api/admin/categories/delete     — 删除（Body 传 id）
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return R.ok(categoryService.list());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody @Valid CategoryCreateRequest req) {
        return R.ok(categoryService.create(req.getName()));
    }

    /** 修改类别（POST 替代原有 PUT） */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody @Valid CategoryUpdateRequest req) {
        return R.ok(categoryService.update(req.getId(), req.getName()));
    }

    /** 删除类别（POST 替代原有 DELETE） */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody @Valid CategoryDeleteRequest req) {
        categoryService.delete(req.getId());
        return R.ok(null);
    }
}
