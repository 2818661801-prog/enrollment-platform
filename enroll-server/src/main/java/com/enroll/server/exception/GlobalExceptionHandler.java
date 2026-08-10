package com.enroll.server.exception;

import com.enroll.server.dto.R;
import com.enroll.server.dto.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * 三层兜底：
 *   1. BusinessException → 200 + 业务码
 *   2. MethodArgumentNotValidException → 200 + 参数校验失败
 *   3. 兜底 Exception → 500（隐藏堆栈，固定文案，详情只打日志）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getResultCode().getCode(), ex.getMessage());
        return ResponseEntity.ok(R.fail(ex.getResultCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return ResponseEntity.ok(R.fail(ResultCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex) {
        // 堆栈详情只打日志，不泄露给前端（可能含 SQL/路径/类名等实现细节）
        log.error("系统异常: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(ResultCode.SYSTEM_ERROR, "服务器开小差了，请稍后重试"));
    }
}
