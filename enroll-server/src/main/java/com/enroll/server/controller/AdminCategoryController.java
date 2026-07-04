package com.enroll.server.controller;

import com.enroll.server.dto.CategoryDTO;
import com.enroll.server.dto.R;
import com.enroll.server.service.CategoryService;
import com.enroll.server.util.RequestUtils;
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
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        CategoryDTO created = categoryService.create(name);
        return R.ok(created);
    }

    /**
     * 修改类别（POST 替代原有 PUT）
     * Body: { id: 1, name: "新名称" }
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        String name = (String) body.get("name");
        CategoryDTO updated = categoryService.update(id, name);
        return R.ok(updated);
    }

    /**
     * 删除类别（POST 替代原有 DELETE）
     * Body: { id: 1 }
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestBody Map<String, Object> body) {
        Integer id = RequestUtils.parseId(body, "id");
        categoryService.delete(id);
        return R.ok(null);
    }
}
