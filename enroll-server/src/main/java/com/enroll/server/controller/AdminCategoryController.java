package com.enroll.server.controller;

import com.enroll.server.dto.CategoryDTO;
import com.enroll.server.dto.R;
import com.enroll.server.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端 - 类别字典管理
 *
 * RESTful：
 *   GET    /api/admin/categories        — 列表
 *   POST   /api/admin/categories        — 新增
 *   PUT    /api/admin/categories/{id}   — 修改
 *   DELETE /api/admin/categories/{id}   — 删除
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

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        CategoryDTO updated = categoryService.update(id, name);
        return R.ok(updated);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return R.ok(null);
    }
}
