package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.service.ClassService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 班级 API 控制器（瘦控制器）
 *
 * RESTful：
 *   GET /api/classes              — 查全部
 *   GET /api/classes/{id}         — 查单个
 *   GET /api/classes?category=xx  — 按类别
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    /** 构造器注入（Spring 推荐，避免字段注入） */
    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /** 查所有 */
    @GetMapping
    public Map<String, Object> list() {
        return R.ok(classService.listAll());
    }

    /** 查单个 */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Integer id) {
        return R.ok(classService.getById(id));
    }

    /** 按类别查 */
    @GetMapping(params = "category")
    public Map<String, Object> listByCategory(@RequestParam String category) {
        return R.ok(classService.listByCategory(category));
    }
}
