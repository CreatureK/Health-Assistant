package org.health.config;

import org.health.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置拦截器、跨域等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 配置拦截器
     * 
     * 注意：拦截器的路径匹配是相对于 context-path (/api/v1) 的
     * 所以 /auth/captcha 实际匹配的是 /api/v1/auth/captcha
     * 
     * 以下接口不需要 Token 认证：
     * - GET /api/v1/auth/captcha - 获取验证码
     * - POST /api/v1/auth/login - 用户登录
     * - POST /api/v1/auth/register - 用户注册
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/captcha", // GET /api/v1/auth/captcha - 获取验证码（无需token）
                        "/auth/login", // POST /api/v1/auth/login - 登录（无需token）
                        "/auth/register", // POST /api/v1/auth/register - 注册（无需token）
                        "/swagger-ui/**", // Swagger UI
                        "/v3/api-docs/**", // Swagger API文档
                        "/swagger-ui.html", // Swagger UI页面
                        "/webjars/**" // Swagger静态资源
                );
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
