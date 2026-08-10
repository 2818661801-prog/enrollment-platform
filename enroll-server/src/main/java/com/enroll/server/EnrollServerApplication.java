package com.enroll.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 报名系统后端启动类
 *
 * @SpringBootApplication 是三合一注解：
 *   @Configuration       — 标记配置类
 *   @EnableAutoConfiguration — 自动配置（根据 pom.xml 的依赖自动装配）
 *   @ComponentScan       — 扫描同包下的 @Controller/@Service/@Repository
 */
@SpringBootApplication
@EnableAsync  // 启用异步，短信发送不阻塞主线程
@EnableScheduling  // 启用定时任务，TimeController.cleanExpired 定期清理过期 IP 防内存泄漏
public class EnrollServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrollServerApplication.class, args);
        // 启动成功后控制台会打印：
        // Started EnrollServerApplication in X.XXX seconds
    }
}
