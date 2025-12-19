package org.health.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.service.auth.AuthService;
import org.health.service.auth.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 认证控制器
 * 处理登录、注册等认证相关接口
 */
@Tag(name = "认证管理", description = "用户登录、注册、验证码等认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 登录接口
     * POST /api/v1/auth/login
     */
    @Operation(summary = "用户登录", description = "通过用户名和密码登录，返回JWT Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = AuthService.LoginResult.class))),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result<AuthService.LoginResult> login(
            @Parameter(description = "登录请求参数", required = true) @Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getCaptchaId(),
                request.getCaptchaCode());
        return Result.success(result);
    }

    /**
     * 注册接口
     * POST /api/v1/auth/register
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    @PostMapping("/register")
    public Result<RegisterResponse> register(
            @Parameter(description = "注册请求参数", required = true) @Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getCaptchaId(),
                request.getCaptchaCode());
        RegisterResponse response = new RegisterResponse();
        response.setId(userId);
        return Result.success(response);
    }

    /**
     * 获取验证码接口
     * GET /api/v1/auth/captcha
     */
    @Operation(summary = "获取验证码", description = "获取图形验证码，用于登录和注册")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = CaptchaResponse.class)))
    })
    @GetMapping("/captcha")
    public Result<CaptchaResponse> getCaptcha() {
        CaptchaService.CaptchaResult captchaResult = captchaService.generateCaptcha();
        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaResult.getCaptchaId());
        response.setImageBase64(captchaResult.getImageBase64());
        response.setExpireIn(captchaResult.getExpireIn());
        return Result.success(response);
    }

    /**
     * 登录请求
     */
    @Schema(description = "登录请求参数")
    public static class LoginRequest {
        @Schema(description = "用户名", example = "admin", required = true)
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Schema(description = "密码", example = "123456", required = true)
        @NotBlank(message = "密码不能为空")
        private String password;

        @Schema(description = "验证码ID", example = "captcha_1234567890")
        private String captchaId;

        @Schema(description = "验证码", example = "ABCD")
        private String captchaCode;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getCaptchaCode() {
            return captchaCode;
        }

        public void setCaptchaCode(String captchaCode) {
            this.captchaCode = captchaCode;
        }
    }

    /**
     * 注册请求
     */
    @Schema(description = "注册请求参数")
    public static class RegisterRequest {
        @Schema(description = "用户名", example = "user123", required = true)
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Schema(description = "密码", example = "123456", required = true)
        @NotBlank(message = "密码不能为空")
        private String password;

        @Schema(description = "验证码ID", example = "captcha_1234567890")
        private String captchaId;

        @Schema(description = "验证码", example = "ABCD")
        private String captchaCode;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getCaptchaCode() {
            return captchaCode;
        }

        public void setCaptchaCode(String captchaCode) {
            this.captchaCode = captchaCode;
        }
    }

    /**
     * 注册响应
     */
    @Schema(description = "注册响应")
    public static class RegisterResponse {
        @Schema(description = "用户ID", example = "1")
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * 验证码响应
     */
    @Schema(description = "验证码响应")
    public static class CaptchaResponse {
        @Schema(description = "验证码ID", example = "captcha_1234567890")
        private String captchaId;

        @Schema(description = "验证码图片Base64编码", example = "data:image/png;base64,...")
        private String imageBase64;

        @Schema(description = "过期时间（秒）", example = "120")
        private Integer expireIn;

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }

        public Integer getExpireIn() {
            return expireIn;
        }

        public void setExpireIn(Integer expireIn) {
            this.expireIn = expireIn;
        }
    }
}
