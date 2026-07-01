package com.enroll.server.dto;

/**
 * 业务响应码枚举
 *
 * 码段规划：
 *   200    — 成功
 *   4001-4099 — 业务校验异常（4xxx 客户端）
 *   5001-5099 — 系统异常（5xxx 服务端）
 *
 * 好处：不用裸写 4001/5000 等魔术数字，统一管理
 *      前端 switch(code) 即可判断业务类型
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    // === 业务异常 4xxx ===
    CLASS_NOT_FOUND(4001, "班级不存在"),
    CLASS_FULL(4002, "该班级名额已满，请选择其他班级"),
    DUPLICATE_APPLICATION(4003, "你已报名过该班级，请勿重复提交"),
    APPLICATION_NOT_FOUND(4004, "报名记录不存在"),
    PARAM_INVALID(4005, "请求参数不合法"),

    // === 系统异常 5xxx ===
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试"),
    DB_ERROR(5001, "数据库异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() { return code; }
    public String getMessage() { return message; }
}
