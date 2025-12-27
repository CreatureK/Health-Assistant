package org.health.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.common.ResultCode;
import org.health.service.ai.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天控制器
 */
@Tag(name = "AI对话", description = "AI聊天对话相关接口")
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    /**
     * 发送消息（流式响应）
     * POST /api/v1/ai/chat
     */
    @Operation(summary = "发送消息", description = "向AI发送消息，支持文本和语音输入，返回流式响应")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "流式响应"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @Parameter(description = "聊天请求参数", required = true) @Valid @RequestBody ChatRequest request) {
        // 创建 SSE 发射器，设置超时时间为 5 分钟
        SseEmitter emitter = new SseEmitter(300000L);

        try {
            AiChatService.ChatRequest serviceRequest = new AiChatService.ChatRequest();
            serviceRequest.setConversationId(request.getConversationId());
            serviceRequest.setMessage(request.getMessage());
            serviceRequest.setInputType(request.getInputType());

            // 调用服务层发送流式消息
            // conversation_id 已在 streamChat 方法中通过 emitter 发送
            aiChatService.streamChat(serviceRequest, emitter);

            // 完成流式响应
            emitter.complete();

        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .data("{\"event\":\"error\",\"message\":\"" + e.getMessage() + "\"}"));
                emitter.completeWithError(e);
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    /**
     * 获取会话列表
     * GET /api/v1/ai/sessions
     */
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有AI会话列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = SessionVO.class)))
    })
    @GetMapping("/sessions")
    public Result<List<SessionVO>> getSessions() {
        List<AiChatService.SessionVO> serviceSessions = aiChatService.getSessions();

        List<SessionVO> sessions = serviceSessions.stream().map(serviceSession -> {
            SessionVO vo = new SessionVO();
            vo.setId(serviceSession.getId());
            vo.setTitle(serviceSession.getTitle());
            vo.setCreatedAt(serviceSession.getCreatedAt());
            vo.setUpdatedAt(serviceSession.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(sessions);
    }

    /**
     * 获取会话消息列表
     * GET /api/v1/ai/sessions/:id/messages
     */
    @Operation(summary = "获取会话消息列表", description = "根据会话ID获取该会话的所有消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    @GetMapping("/sessions/{id}/messages")
    public Result<List<MessageVO>> getMessages(
            @Parameter(description = "会话ID（conversation_id）", required = true) @PathVariable String id) {
        try {
            List<AiChatService.MessageVO> serviceMessages = aiChatService.getMessages(id);

            List<MessageVO> messages = serviceMessages.stream().map(serviceMessage -> {
                MessageVO vo = new MessageVO();
                vo.setRole(serviceMessage.getRole());
                vo.setContent(serviceMessage.getContent());
                vo.setInputType(serviceMessage.getInputType());
                vo.setSafetyHint(serviceMessage.getSafetyHint());
                vo.setCreatedAt(serviceMessage.getCreatedAt());
                return vo;
            }).collect(Collectors.toList());

            return Result.success(messages);
        } catch (RuntimeException e) {
            return Result.error(ResultCode.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 聊天请求
     */
    @Schema(description = "聊天请求参数")
    public static class ChatRequest {
        @Schema(description = "会话ID（可选，为空则创建新会话）", example = "10799fb8-64f7-4296-bbf7-b42bfbe0ae54")
        private String conversationId;

        @Schema(description = "消息内容", example = "你好", required = true)
        @NotBlank(message = "消息内容不能为空")
        private String message;

        @Schema(description = "输入类型", example = "text", allowableValues = { "text", "voice" }, required = true)
        @NotBlank(message = "输入类型不能为空")
        @Pattern(regexp = "text|voice", message = "输入类型必须是text或voice")
        private String inputType;

        // Getters and Setters
        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getInputType() {
            return inputType;
        }

        public void setInputType(String inputType) {
            this.inputType = inputType;
        }
    }

    /**
     * 会话视图对象
     */
    @Schema(description = "会话视图对象")
    public static class SessionVO {
        @Schema(description = "会话ID", example = "10799fb8-64f7-4296-bbf7-b42bfbe0ae54")
        private String id;

        @Schema(description = "会话标题", example = "健康咨询")
        private String title;

        @Schema(description = "创建时间", example = "2025-01-01T10:00:00")
        private java.time.LocalDateTime createdAt;

        @Schema(description = "更新时间", example = "2025-01-01T10:00:00")
        private java.time.LocalDateTime updatedAt;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 消息视图对象
     */
    @Schema(description = "消息视图对象")
    public static class MessageVO {
        @Schema(description = "角色", example = "user", allowableValues = { "user", "assistant" })
        private String role;

        @Schema(description = "消息内容", example = "你好")
        private String content;

        @Schema(description = "输入类型", example = "text", allowableValues = { "text", "voice" })
        private String inputType;

        @Schema(description = "安全提示", example = "请注意健康信息仅供参考")
        private String safetyHint;

        @Schema(description = "创建时间", example = "2025-01-01T10:00:00")
        private java.time.LocalDateTime createdAt;

        // Getters and Setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getInputType() {
            return inputType;
        }

        public void setInputType(String inputType) {
            this.inputType = inputType;
        }

        public String getSafetyHint() {
            return safetyHint;
        }

        public void setSafetyHint(String safetyHint) {
            this.safetyHint = safetyHint;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
