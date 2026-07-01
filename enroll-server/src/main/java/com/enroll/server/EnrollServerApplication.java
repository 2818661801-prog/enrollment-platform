package com.enroll.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 报名系统后端启动类
 *
 * @SpringBootApplication 是三合一注解：
 *   @Configuration       — 标记配置类
 *   @EnableAutoConfiguration — 自动配置（根据 pom.xml 的依赖自动装配）
 *   @ComponentScan       — 扫描同包下的 @Controller/@Service/@Repository
 */
@SpringBootApplication
public class EnrollServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollServerApplication.class, args);
        // 启动成功后控制台会打印：
        // Started EnrollServerApplication in X.XXX seconds
    }
}
