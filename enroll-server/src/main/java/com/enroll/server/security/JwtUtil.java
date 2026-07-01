package com.enroll.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * JWT 结构：header.payload.signature
 *   header  = 算法 + 类型
 *   payload = 用户信息（subject=身份证号，自定义 claim）
 *   signature = 用密钥签名，防伪造
 *
 * HS256 算法用对称密钥（同一密钥加密 + 解密）
 *
 * 用法：
 *   String token = jwtUtil.generate("***REMOVED***");
 *   Claims claims = jwtUtil.parse(token);
 *   String idCard = claims.getSubject();
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /** 构造签名密钥（HS256 需要 ≥ 256 bit = 32 byte） */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token
     * @param idCard 学生身份证号（作为 subject）
     * @return 签名的 JWT 字符串
     */
    public String generate(String idCard) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "student");  // 角色：student / admin
        return Jwts.builder()
                .claims(claims)
                .subject(idCard)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token
     * @param token JWT 字符串
     * @return Claims（含 subject + 自定义字段）
     * @throws RuntimeException 签名错误或过期
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
