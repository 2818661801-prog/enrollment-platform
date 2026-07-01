package com.enroll.server.controller;

import com.enroll.server.dto.R;
import com.enroll.server.service.ClassService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 班级 API 控制器（瘦控制器）
 *
 * RESTful：
 *   GET /api/classes      — 查全部
 *   GET /api/classes/{id} — 查单个
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

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
}
