package com.enroll.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置 — 允许前端 (localhost:5173) 调用后端 API (localhost:8080)
 *
 * 为什么要跨域？
 *   浏览器安全策略：localhost:5173 的 JS 不能随意请求 localhost:8080
 *   （端口不同 = 不同源 → 浏览器拦截）
 *
 * 解决方案：
 *   后端在响应头里加 Access-Control-Allow-Origin，告诉浏览器"我允许前端跨域"
 *
 * 开发环境用这个类，部署后前端和后端同域名则不需要。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许哪些前端地址跨域访问
        config.addAllowedOriginPattern("http://localhost:5173");
        config.addAllowedOriginPattern("http://localhost:*");    // 任何本地端口

        // 允许携带 Cookie / Authorization header
        config.setAllowCredentials(true);

        // 允许所有 HTTP 方法（GET、POST、PUT、DELETE、OPTIONS）
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 只对 /api/** 路径生效
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
