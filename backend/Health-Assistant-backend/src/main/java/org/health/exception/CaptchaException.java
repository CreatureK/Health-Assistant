package org.health.exception;

/**
 * 验证码异常类
 */
public class CaptchaException extends RuntimeException {

  public CaptchaException(String message) {
    super(message);
  }

  public CaptchaException(String message, Throwable cause) {
    super(message, cause);
  }
}
