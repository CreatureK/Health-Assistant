package org.health.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.UserContext;
import org.health.mapper.UserMapper;
import org.health.entity.User;
import org.health.service.ai.DifyClientService;
import org.health.service.ai.EmitterAlreadyCompletedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Map;

/**
 * AI聊天控制器 - 完全按照 DIFY API 规范
 */
@Tag(name = "AI对话", description = "AI聊天对话接口，完全符合 DIFY API 规范")
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private DifyClientService difyClientService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 发送消息（流式响应）
     * POST /api/v1/ai/chat-messages
     * 完全按照 DIFY API 规范：https://docs.dify.ai/api-reference/chat-messages
     */
    @Operation(summary = "发送消息", description = "向 DIFY 应用发送用户消息，返回流式响应")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "流式响应 (text/event-stream)"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping(value = "/chat-messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatMessages(
            @Parameter(description = "聊天请求参数，完全符合 DIFY API 规范", required = true) @Valid @RequestBody ChatMessageRequest request) {

        // 创建 SSE 发射器，设置超时时间为 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);

        try {
            // 确定使用的用户名：优先使用请求体中的username，否则从token中获取
            String username;
            if (request.getUsername() != null && !request.getUsername().isEmpty()) {
                username = request.getUsername();
            } else {
                // 从token中获取用户信息
                Long userId = UserContext.getUserId();
                User user = userMapper.selectById(userId);
                if (user == null || user.getUsername() == null) {
                    throw new RuntimeException("用户不存在或用户名无效");
                }
                username = user.getUsername();
            }

            // 调用 DIFY API 发送流式消息
            // user 参数使用 username，其他参数直接透传
            difyClientService.streamChat(
                    request.getQuery(),
                    request.getConversationId(),
                    username,
                    request.getInputs(),
                    request.getAutoGenerateName(),
                    emitter);

            // 正常完成流式响应
            try {
                emitter.complete();
            } catch (IllegalStateException e) {
                // emitter 可能已经被完成，忽略
            }

        } catch (EmitterAlreadyCompletedException e) {
            // emitter 已经在服务层完成，不需要再次处理
        } catch (Exception e) {
            // 只有在 emitter 未被完成时才处理
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> errorData = Map.of(
                        "event", "error",
                        "status", 500,
                        "code", "internal_error",
                        "message", e.getMessage() != null ? e.getMessage() : "系统错误");
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorData)));
                emitter.completeWithError(e);
            } catch (IOException | IllegalStateException ex) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // 忽略所有错误
                }
            }
        }

        return emitter;
    }

    /**
     * 聊天请求 - 完全按照 DIFY API 规范
     */
    @Schema(description = "聊天请求参数，完全符合 DIFY API 规范")
    public static class ChatMessageRequest {
        @Schema(description = "用户输入/提问内容", example = "你好", required = true)
        @NotBlank(message = "query 不能为空")
        private String query;

        @Schema(description = "响应模式：streaming 或 blocking", example = "streaming", allowableValues = { "streaming",
                "blocking" })
        private String responseMode = "streaming";

        @Schema(description = "会话ID（可选，为空则创建新会话）", example = "10799fb8-64f7-4296-bbf7-b42bfbe0ae54")
        private String conversationId;

        @Schema(description = "App 定义的变量值，键值对形式", example = "{}")
        private Map<String, Object> inputs;

        @Schema(description = "是否自动生成会话标题", example = "true")
        private Boolean autoGenerateName;

        @Schema(description = "用户名", example = "user@example.com")
        private String username;

        // Getters and Setters
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getResponseMode() {
            return responseMode;
        }

        public void setResponseMode(String responseMode) {
            this.responseMode = responseMode;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public Map<String, Object> getInputs() {
            return inputs;
        }

        public void setInputs(Map<String, Object> inputs) {
            this.inputs = inputs;
        }

        public Boolean getAutoGenerateName() {
            return autoGenerateName;
        }

        public void setAutoGenerateName(Boolean autoGenerateName) {
            this.autoGenerateName = autoGenerateName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
