package com.enroll.server.service;

import com.enroll.server.dto.ResultCode;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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

    private static final String SMS_KEY_PREFIX = "sms:login:";
    private static final String SMS_IP_PREFIX  = "sms:ip:";
    private static final int CODE_TTL_SECONDS = 300; // 5分钟
    private static final int MAX_RETRY = 5;         // 错误超过5次需重新获取
    private static final int SMS_SEND_GAP = 60;     // S11: 同一手机号发送间隔 60 秒
    private static final int SMS_IP_LIMIT  = 10;    // S14: 同一 IP 每分钟最多发 10 次

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    @Value("${sms.sn}")
    private String smsSn;

    @Value("${sms.pwd}")
    private String smsPwd;

    @Value("${sms.url}")
    private String smsUrl;

    @Value("${sms.sign}")
    private String smsSign;

    public AuthService(StringRedisTemplate redis, JwtUtil jwtUtil) {
        this.redis = redis;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 发送验证码（存入 Redis，支持重复发送刷新 TTL）
     * ⚠️ S11 修复：手机号 60 秒内不能重复发
     * ⚠️ S14 修复：同一 IP 每分钟最多发 10 次
     * ⚠️ S12 修复：日志不打印 code 明文
     * @param phone 11位手机号
     */
    public void sendCode(String phone) {
        validatePhone(phone);

        // ===== S11: 手机号频率限制（60秒内不能重复发） =====
        String phoneGapKey = "sms:gap:" + phone;
        Boolean gapSet = redis.opsForValue().setIfAbsent(phoneGapKey, "1", SMS_SEND_GAP, TimeUnit.SECONDS);
        if (gapSet == null || !gapSet) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "发送太频繁，请" + SMS_SEND_GAP + "秒后再试");
        }

        // ===== S14: IP 频率限制（每分钟最多 10 次） =====
        String clientIp = getClientIp();
        String ipKey = SMS_IP_PREFIX + clientIp;
        Long ipCount = redis.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1) {
            // 第一次调用这个 key，设 60 秒过期
            redis.expire(ipKey, 60, TimeUnit.SECONDS);
        }
        if (ipCount != null && ipCount > SMS_IP_LIMIT) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "请求过于频繁，请稍后再试");
        }

        // 生成6位随机数字验证码（100000~999999）
        SecureRandom sr = new SecureRandom();
        int code = sr.nextInt(900000) + 100000;
        String codeStr = String.valueOf(code);

        String key = SMS_KEY_PREFIX + phone;
        redis.opsForValue().set(key, codeStr, CODE_TTL_SECONDS, TimeUnit.SECONDS);

        sendSmsAsync(phone, codeStr);

        // S12 修复：日志不打印 code 明文，只打印手机号和 IP（脱敏）
        // log.info("【验证码已发送】phone={} ip={} codeLen={}", maskPhone(phone), clientIp, codeStr.length());
    }

    /** 提取客户端真实 IP（注意反向代理场景） */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            // 优先取 X-Forwarded-For（反向代理），否则取 remoteAddr
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // 多级代理时取第一个 IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** 手机号脱敏（中间4位） */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
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
        // log.info("【学生登录成功】phone={}", phone);
        return token;
    }

    /**
     * 异步发送短信（真实接口：大国三通短信平台）
     * 异步：验证码已写 Redis 返给用户，短信在后台线程发送，不阻塞主流程
     * @param phone 收件人手机号
     * @param code  验证码
     */
    @Async
    private void sendSmsAsync(String phone, String code) {
        // 大汉三通短信平台：GET /mdsmssend.ashx?sn=...&pwd=...&mobile=...&content=...
        // content 内容需与大汉三通平台报备的模板格式一致，平台会自动拼接签名
        // ⚠️ P0-1 修复：SMS 账号密码从 application.yml 注入，生产通过环境变量覆盖
        String content = smsSign + "您的验证码为" + code + "，5分钟内有效，请勿泄露给他人。";
        String encodedContent = java.net.URLEncoder.encode(content, StandardCharsets.UTF_8);
        String url = String.format(
            smsUrl + "?sn=%s&pwd=%s&mobile=%s&content=%s",
            smsSn, smsPwd, phone, encodedContent
        );
        try {
            String resp = restTemplate.getForObject(url, String.class);
            // log.info("【短信发送结果】phone={} resp={}", maskPhone(phone), resp);
        } catch (Exception e) {
            // log.error("【短信发送失败】phone={}", maskPhone(phone), e);
        }
    }

    private final RestTemplate restTemplate;
    {
        // 配置超时：连接8秒，读取15秒，防止短信平台响应慢时阻塞线程
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(8));
        factory.setReadTimeout(java.time.Duration.ofSeconds(15));
        this.restTemplate = new org.springframework.web.client.RestTemplate(factory);
    }

    /** 手机号格式校验 */
    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确");
        }
    }
}
