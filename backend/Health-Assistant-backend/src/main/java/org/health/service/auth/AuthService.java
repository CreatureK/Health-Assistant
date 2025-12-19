package org.health.service.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import org.health.common.JwtUtil;
import org.health.common.ResultCode;
import org.health.entity.User;
import org.health.exception.BusinessException;
import org.health.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 用户登录
     *
     * @param username    用户名
     * @param password    密码
     * @param captchaId   验证码ID
     * @param captchaCode 验证码
     * @return Token和用户信息
     */
    public LoginResult login(String username, String password, String captchaId, String captchaCode) {
        // 验证验证码
        captchaService.validateCaptcha(captchaId, captchaCode);

        // 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 验证密码（这里简化处理，实际应该使用BCrypt等加密算法）
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId());

        // 返回结果
        LoginResult result = new LoginResult();
        result.setToken(token);
        LoginResult.UserInfo userInfo = new LoginResult.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());
        result.setUser(userInfo);

        return result;
    }

    /**
     * 用户注册
     *
     * @param username    用户名
     * @param password    密码
     * @param captchaId   验证码ID
     * @param captchaCode 验证码
     * @return 用户ID
     */
    public Long register(String username, String password, String captchaId, String captchaCode) {
        // 验证验证码
        captchaService.validateCaptcha(captchaId, captchaCode);

        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        // 密码加密（这里简化处理，实际应该使用BCrypt等加密算法）
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setRole("elder"); // 默认角色为老人
        // 设置默认nickname（使用username）
        user.setNickname(username);

        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 登录结果
     */
    @Schema(description = "登录响应结果")
    public static class LoginResult {
        @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzUxMiJ9...")
        private String token;

        @Schema(description = "用户信息")
        private UserInfo user;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public UserInfo getUser() {
            return user;
        }

        public void setUser(UserInfo user) {
            this.user = user;
        }

        @Schema(description = "用户信息")
        public static class UserInfo {
            @Schema(description = "用户ID", example = "1")
            private Long id;

            @Schema(description = "昵称", example = "张三")
            private String nickname;

            @Schema(description = "角色", example = "elder", allowableValues = { "elder", "family" })
            private String role;

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getNickname() {
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }
        }
    }
}
