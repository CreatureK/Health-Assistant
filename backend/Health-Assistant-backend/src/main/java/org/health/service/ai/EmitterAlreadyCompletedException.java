package org.health.service.ai;

/**
 * 标记 SseEmitter 已经被完成的异常
 * 用于避免在 Controller 层重复完成 emitter
 */
public class EmitterAlreadyCompletedException extends RuntimeException {
  public EmitterAlreadyCompletedException(String message) {
    super(message);
  }

  public EmitterAlreadyCompletedException(String message, Throwable cause) {
    super(message, cause);
  }
}
