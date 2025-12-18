package org.health;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 健康助手后端应用启动类
 *
 * @author Health Assistant Team
 */
@SpringBootApplication
@MapperScan("org.health.mapper")
public class HealthAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthAssistantApplication.class, args);
    }
}

