package com.enroll.server.config;

import com.enroll.server.security.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（拦截器 + CORS）
 *
 * 拦截器策略：
 *   - /api/admin/**  → JWT 鉴权（后台接口）
 *   - /api/classes、/api/applications → 不拦截（学生匿名）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 构造器注入 */
    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/admin/**")  // 只拦截后台
                .excludePathPatterns(
                    "/api/classes/**",
                    "/api/applications/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ⚠️ S6 修复：生产环境必须改成实际的外网域名，禁止 *
        // 本地调试可以加 localhost，生产用环境变量注入
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS") != null
            ? System.getenv("CORS_ALLOWED_ORIGINS")
            : "http://localhost:5173,http://127.0.0.1:5173";
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);  // preflight 缓存 1 小时
    }
}
