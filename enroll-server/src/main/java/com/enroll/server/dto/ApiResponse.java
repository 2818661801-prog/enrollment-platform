package com.enroll.server.dto;

/**
 * 统一 API 响应包装（保留兼容，新代码推荐用 R.java）
 *
 * 设计目的：所有接口返回结构一致，前端只需解析一个壳
 *
 * 响应结构：
 * {
 *   "code": 200,           // 业务码（200=成功，4xxx=业务异常，5xxx=系统异常）
 *   "message": "操作成功",  // 提示信息
 *   "data": {...}          // 业务数据（可为 null）
 * }
 *
 * 为什么不直接用 HTTP 状态码：
 *   HTTP 状态码只能表示粗粒度结果（200/4xx/5xx），
 *   业务码可以精确表达"班级已满""重复报名"等具体原因
 */
public class ApiResponse<T> {

    /** 业务码 */
    private Integer code;

    /** 提示信息（用户可读） */
    private String message;

    /** 业务数据（失败时为 null） */
    private T data;

    // ==================== 构造器 ====================

    public ApiResponse() {}

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== Getter / Setter ====================

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // ==================== 静态工厂（推荐用这些方法创建） ====================

    /** 成功（无数据） */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功（自定义消息） */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /** 失败（用枚举） */
    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（自定义消息，会覆盖枚举默认） */
    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }
}
