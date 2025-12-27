package org.health;

import io.github.cdimascio.dotenv.Dotenv;
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
        // 加载 .env 文件到系统环境变量
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        
        // 将 .env 中的变量设置到系统环境变量中
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        SpringApplication.run(HealthAssistantApplication.class, args);
    }
}

