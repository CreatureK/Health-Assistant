package org.health.interceptor;

import org.health.common.JwtUtil;
import org.health.common.ResultCode;
import org.health.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证拦截器
 * 验证Bearer Token，并将用户ID存入UserContext
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        // 提取Token
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        // 获取用户ID并存入上下文
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除上下文
        UserContext.clear();
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}", 
                        resultCode.getCode(), resultCode.getMsg())
        );
    }
}

