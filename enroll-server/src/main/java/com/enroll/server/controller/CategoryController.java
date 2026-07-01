package com.enroll.server.controller;

import com.enroll.server.dto.CategoryDTO;
import com.enroll.server.dto.R;
import com.enroll.server.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 - 类别字典（只读，供表单下拉使用）
 *   GET /api/categories — 列表
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Map<String, Object> list() {
        return R.ok(categoryService.list());
    }
}
