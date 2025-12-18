package org.health.config;

import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 * Spring Boot的MyBatis Starter已经自动配置了大部分内容
 * 此配置类用于额外的自定义配置（如果需要）
 * 
 * 注意：@MapperScan已在启动类HealthAssistantApplication中配置，此处不需要重复配置
 */
@Configuration
public class MyBatisConfig {
    // 如果需要自定义SqlSessionFactory，可以在这里配置
    // 但通常Spring Boot的自动配置已经足够
}
