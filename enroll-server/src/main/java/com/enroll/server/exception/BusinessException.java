package com.enroll.server.exception;

import com.enroll.server.dto.ResultCode;

/**
 * 业务异常
 *
 * 用途：Service 层抛出业务级别的异常（如"班级已满""重复报名"），
 *      由 GlobalExceptionHandler 统一捕获并返回友好的 API 响应
 *
 * vs RuntimeException：
 *   裸 RuntimeException → 500 + 堆栈（用户看不懂、暴露系统信息）
 *   BusinessException → 200 + 业务码 + 中文提示（友好、精准）
 *
 * 使用：
 *   if (cls.getEnrolled() >= cls.getQuota()) {
 *       throw new BusinessException(ResultCode.CLASS_FULL);
 *   }
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /** 自定义消息（覆盖枚举默认） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    /** 获取业务码枚举（供 GlobalExceptionHandler 使用） */
    public ResultCode getResultCode() {
        return resultCode;
    }
}
