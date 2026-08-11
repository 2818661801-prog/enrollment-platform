package com.enroll.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 身份证校验码纯逻辑测试（GB 11643-1999）
 *
 * 不依赖 Spring 上下文，直接测算法正确性。
 * 与 ApplicationService.validateIdCardChecksum 逻辑一致（复制出来测）。
 */
public class IdCardChecksumTest {

    // 权重和校验码映射表（与 ApplicationService 一致）
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private boolean validateIdCardChecksum(String idCard) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHTS[i];
        }
        return Character.toUpperCase(idCard.charAt(17)) == CODES[sum % 11];
    }

    @Test
    void 合法身份证_校验码正确() {
        assertTrue(validateIdCardChecksum("110101***REMOVED***7"));
    }

    @Test
    void 合法身份证_末位X大写() {
        assertTrue(validateIdCardChecksum("110101***REMOVED***X"));
    }

    @Test
    void 合法身份证_末位x小写() {
        assertTrue(validateIdCardChecksum("110101***REMOVED***x"));
    }

    @Test
    void 非法身份证_校验码错误() {
        assertFalse(validateIdCardChecksum("110101***REMOVED***3"));
    }

    @Test
    void 非法身份证_末位Y不在映射表() {
        assertFalse(validateIdCardChecksum("110101***REMOVED***Y"));
    }

    @Test
    void 校验码映射表_全量验证() {
        // 验证映射表本身：sum%11 的每个值都有对应校验码
        assertEquals(11, CODES.length);
        // 0→'1', 1→'0', 2→'X', 3→'9', ...
        assertEquals('1', CODES[0]);
        assertEquals('0', CODES[1]);
        assertEquals('X', CODES[2]);
    }
}
