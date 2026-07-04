package com.enroll.server.util;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.exception.BusinessException;

import java.util.Map;

/**
 * 请求解析工具类
 *
 * P0-4: 统一 ID 解析，解决各 Controller 从 Map 强转 Integer 时
 *       字符串 "1" 导致 ClassCastException 的问题
 */
public final class RequestUtils {

    private RequestUtils() {}

    /**
     * 从请求 Body（Map）中安全解析 Integer ID
     *
     * 支持：
     *   - Integer 类型：直接返回
     *   - String 类型：自动转换（"1" → 1）
     *   - null / 空：抛业务异常
     *
     * @param body 请求参数 Map
     * @param key  参数名
     * @return 解析后的 Integer ID
     * @throws BusinessException 参数为空/格式错误时抛出
     */
    public static Integer parseId(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, key + " 不能为空");
        }
        if (val instanceof Integer) {
            return (Integer) val;
        }
        if (val instanceof String) {
            String s = ((String) val).trim();
            if (s.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, key + " 不能为空");
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.PARAM_INVALID, key + " 必须是数字");
            }
        }
        throw new BusinessException(ResultCode.PARAM_INVALID, key + " 格式错误");
    }

    /**
     * 安全解析 Integer（可为空）
     *
     * @param body 请求参数 Map
     * @param key  参数名
     * @param required 是否强制要求有值（true=不能为null，false=可以返回null）
     * @return 解析后的值，required=true 时参数为空抛异常
     */
    public static Integer parseId(Map<String, Object> body, String key, boolean required) {
        Object val = body.get(key);
        if (val == null) {
            if (required) {
                throw new BusinessException(ResultCode.PARAM_INVALID, key + " 不能为空");
            }
            return null;
        }
        if (val instanceof Integer) {
            return (Integer) val;
        }
        if (val instanceof String) {
            String s = ((String) val).trim();
            if (s.isEmpty()) {
                if (required) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, key + " 不能为空");
                }
                return null;
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.PARAM_INVALID, key + " 必须是数字");
            }
        }
        throw new BusinessException(ResultCode.PARAM_INVALID, key + " 格式错误");
    }
}
