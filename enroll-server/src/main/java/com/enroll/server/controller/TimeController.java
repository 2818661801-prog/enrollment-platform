package com.enroll.server.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务器时间接口（无需鉴权，学生端前端用来校正本地时间）
 *
 * GET /api/time — 返回服务器当前时间戳（毫秒）
 *
 * 为什么后端直接提供时间：
 * ① 服务器在大陆，时区即北京时间，不需要第三方 API
 * ② 前端浏览器时间可被用户篡改，用服务器时间做基准更可靠
 * ③ 前端计算"本地时间-服务器时间"偏差，后续判断都用 trustedNow()
 */
@RestController
@RequestMapping("/api")
public class TimeController {

    @GetMapping("/time")
    public Map<String, Object> getServerTime() {
        Map<String, Object> result = new HashMap<>();
        result.put("serverTime", System.currentTimeMillis());
        return result;
    }
}
