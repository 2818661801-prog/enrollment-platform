package com.enroll.server.service;

import com.enroll.server.entity.SysConfig;
import com.enroll.server.repository.SysConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 *
 * 功能：
 *   1. 发送验证码（6位数字，5分钟有效）
 *   2. 验证手机号+验证码，返 JWT
 *
 * 验证码存储：内存 Map（重启后丢失，生产环境建议换 Redis）
 *   key = 手机号
 *   value = {code, expireAt}
 *
 * SMS 发送：目前是 mock（控制台日志），主人可替换为真实 SMS 服务
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 验证码有效期：5分钟（毫秒） */
    private static final long CODE_TTL_MS = 5 * 60 * 1000;

    /** 内存验证码存储：手机号 → {code, expireAt} */
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    private final SysConfigRepository sysConfigRepo;
    private final Random random = new Random();

    public AuthService(SysConfigRepository sysConfigRepo) {
        this.sysConfigRepo = sysConfigRepo;
    }

    // ==================== 公开方法 ====================

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 发送结果描述（方便调试时从日志里复制验证码）
     */
    public String sendCode(String phone) {
        // 1. 生成 6 位数字验证码
        String code = String.format("%06d", random.nextInt(1_000_000));
        long expireAt = System.currentTimeMillis() + CODE_TTL_MS;
        codeStore.put(phone, new CodeEntry(code, expireAt));

        // 2. Mock 发送（控制台日志），主人可替换为真实 SMS API
        log.info("========== 【Mock SMS】发送验证码 ==========");
        log.info("  收件人：{}", phone);
        log.info("  验证码：{}（5分钟内有效）", code);
        log.info("  用途  ：管理员登录");
        log.info("==========================================");

        return code; // 调试用，返回 code；生产去掉此行
    }

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 验证成功返回 true，失败返回 false
     */
    public boolean verifyCode(String phone, String code) {
        CodeEntry entry = codeStore.get(phone);
        if (entry == null) {
            log.debug("验证码不存在或已过期：phone={}", phone);
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            codeStore.remove(phone);
            log.debug("验证码已过期：phone={}", phone);
            return false;
        }
        if (!entry.code.equals(code)) {
            log.debug("验证码错误：phone={}, input={}, actual={}", phone, code, entry.code);
            return false;
        }
        // 验证成功，删除验证码（一次性）
        codeStore.remove(phone);
        log.debug("验证码验证成功：phone={}", phone);
        return true;
    }

    /**
     * 获取管理员手机号（从 sys_config 读取）
     * 如果未配置，返回默认手机号供测试用
     */
    public String getAdminPhone() {
        return sysConfigRepo.findByCfgKey("admin_phone")
                .map(SysConfig::getCfgValue)
                .orElse("***REMOVED***"); // 测试用默认值
    }

    // ==================== 内部类 ====================

    /** 验证码条目 */
    private static class CodeEntry {
        final String code;
        final long expireAt;

        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
