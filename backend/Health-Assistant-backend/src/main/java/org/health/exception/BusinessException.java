package org.health.exception;

import org.health.common.ResultCode;

/**
 * 业务异常类
 * 用于处理业务逻辑错误，支持自定义错误码
 */
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /**
     * 构造函数
     *
     * @param resultCode 错误码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    /**
     * 构造函数（自定义消息）
     *
     * @param resultCode 错误码
     * @param message    自定义错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    /**
     * 构造函数（带原因）
     *
     * @param resultCode 错误码
     * @param message    错误消息
     * @param cause      原因
     */
    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public ResultCode getResultCode() {
        return resultCode;
    }
}

