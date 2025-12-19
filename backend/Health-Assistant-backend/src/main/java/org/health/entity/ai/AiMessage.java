package org.health.entity.ai;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI消息实体类
 */
@Data
public class AiMessage {
    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 角色：user-用户，assistant-AI助手
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 输入类型：text-文本，voice-语音
     */
    private String inputType;

    /**
     * 安全提示（AI回复时可能包含）
     */
    private String safetyHint;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

