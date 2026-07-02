package com.enroll.server.service;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 学生端认证服务（验证码登录）
 *
 * 流程：
 *   1. POST /api/auth/send-code {phone} → 生成6位验证码存Redis，调短信接口
 *   2. POST /api/auth/login/sms {phone, code} → Redis校验成功 → 返JWT
 *
 * Redis Key：sms:login:{phone} → 验证码，TTL 5分钟
 *
 * SMS 发送：2026-07-02 接入大汉三通短信平台（http://***REMOVED***:8061）
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String SMS_KEY_PREFIX = "sms:login:";
    private static final int CODE_TTL_SECONDS = 300; // 5分钟
    private static final int MAX_RETRY = 5;         // 错误超过5次需重新获取

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    public AuthService(StringRedisTemplate redis, JwtUtil jwtUtil) {
        this.redis = redis;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 发送验证码（存入 Redis，支持重复发送刷新 TTL）
     * @param phone 11位手机号
     */
    public void sendCode(String phone) {
        validatePhone(phone);

        // 生成6位随机数字验证码（100000~999999）
        int code = (int) (Math.random() * 900000 + 100000);
        String codeStr = String.valueOf(code);

        String key = SMS_KEY_PREFIX + phone;
        redis.opsForValue().set(key, codeStr, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        // TODO: 主人提供短信接口后，替换下面这行
        sendSms(phone, codeStr);

        log.info("【验证码已发送】phone={} code={}", phone, codeStr);
    }

    /**
     * 校验验证码，验证成功返回 JWT
     * @param phone 手机号
     * @param code  用户输入的6位验证码
     * @return JWT（sub=手机号，role=student）
     */
    public String verifyCodeAndLogin(String phone, String code) {
        validatePhone(phone);

        String key = SMS_KEY_PREFIX + phone;
        String storedCode = redis.opsForValue().get(key);

        // 验证码不存在（过期或未发送）
        if (storedCode == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "验证码已过期，请重新获取");
        }

        // 验证码错误
        if (!storedCode.equals(code.trim())) {
            // 错误计数（防止暴力穷举）
            String errKey = "sms:err:" + phone;
            Long errCount = redis.opsForValue().increment(errKey);
            redis.expire(errKey, CODE_TTL_SECONDS, TimeUnit.SECONDS);
            if (errCount != null && errCount >= MAX_RETRY) {
                redis.delete(key);
                redis.delete(errKey);
                throw new BusinessException(ResultCode.PARAM_INVALID, "验证码错误次数过多，请重新获取");
            }
            throw new BusinessException(ResultCode.PARAM_INVALID, "验证码错误");
        }

        // 验证成功，删除验证码（一次性使用）
        redis.delete(key);
        redis.delete("sms:err:" + phone);

        // 生成学生 JWT
        String token = jwtUtil.generateStudent(phone);
        log.info("【学生登录成功】phone={}", phone);
        return token;
    }

    /**
     * 发送短信（真实接口：大国三通短信平台）
     * @param phone 收件人手机号
     * @param code  验证码
     */
    private void sendSms(String phone, String code) {
        // 大汉三通短信平台：GET /mdsmssend.ashx?sn=...&pwd=...&mobile=...&content=...
        // content 内容需与大汉三通平台报备的模板格式一致，平台会自动拼接签名
        String sn     = "SDK-BBX-010-39560";
        String pwd    = "3365CB7EB11EEC171571E8E91C61EF30";
        // 与大汉三通平台模板格式一致：签名+正文（平台配置模板时已设定签名拼接规则）
        String content = "【杭州电子科技大学信息工程学院】您的验证码为" + code + "，5分钟内有效，请勿泄露给他人。";
        String encodedContent = java.net.URLEncoder.encode(content, StandardCharsets.UTF_8);
        String url = String.format(
            "http://***REMOVED***:8061/mdsmssend.ashx?sn=%s&pwd=%s&mobile=%s&content=%s",
            sn, pwd, phone, encodedContent
        );
        try {
            String resp = restTemplate.getForObject(url, String.class);
            log.info("【短信发送结果】phone={} code={} resp={}", phone, code, resp);
        } catch (Exception e) {
            log.error("【短信发送失败】phone={} code={}", phone, code, e);
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();

    /** 手机号格式校验 */
    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确");
        }
    }
}
