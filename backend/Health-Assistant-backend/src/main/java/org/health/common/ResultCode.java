package org.health.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 符合接口文档规范
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    /**
     * 成功
     */
    SUCCESS(0, "ok"),

    /**
     * 参数错误
     */
    BAD_REQUEST(400, "参数错误"),

    /**
     * 未登录 / Token 失效
     */
    UNAUTHORIZED(401, "未登录或Token失效"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 冲突（如重复创建record点位）
     */
    CONFLICT(409, "资源冲突"),

    /**
     * 服务器错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器错误");

    private final Integer code;
    private final String msg;
}

