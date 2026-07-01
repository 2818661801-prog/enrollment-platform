package com.enroll.server.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一响应工具类
 *
 * 为什么不直接用泛型 ApiResponse<T>：
 *   Spring MVC + Jackson 对泛型类的序列化在某些 JDK 版本下存在
 *   Content-Type 协商问题（406/500）。改用 Map 完全避开泛型擦除。
 *
 * 响应结构（所有接口一致）：
 * {
 *   "code": 200,           // 业务码
 *   "message": "操作成功",  // 提示信息
 *   "data": {...}          // 业务数据（失败时为 null）
 * }
 */
public final class R {

    private R() {} // 工具类禁止实例化

    /** 成功（无数据） */
    public static Map<String, Object> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功（带数据） */
    public static Map<String, Object> ok(Object data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功（自定义消息） */
    public static Map<String, Object> ok(String message, Object data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    /** 失败（用枚举） */
    public static Map<String, Object> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（自定义消息） */
    public static Map<String, Object> fail(ResultCode resultCode, String message) {
        return build(resultCode.getCode(), message, null);
    }

    private static Map<String, Object> build(int code, String message, Object data) {
        Map<String, Object> map = new LinkedHashMap<>(); // 保持插入顺序
        map.put("code", code);
        map.put("message", message);
        map.put("data", data);
        return map;
    }
}
