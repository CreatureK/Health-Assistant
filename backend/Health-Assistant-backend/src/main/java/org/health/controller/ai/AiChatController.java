package org.health.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.health.common.Result;
import org.health.common.ResultCode;
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
import java.util.ArrayList;
import java.util.List;
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
     * 获取会话列表
     * GET /api/v1/ai/conversations
     * 完全按照 DIFY API 规范：https://docs.dify.ai/api-reference/conversations
     */
    @Operation(summary = "获取会话列表", description = "获取指定用户的会话列表，支持分页与排序")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回会话列表"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录或Token失效")
    })
    @GetMapping("/conversations")
    public Result<ConversationListResponse> getConversations(
            @Parameter(description = "分页游标：当前页最后一条记录的 id", required = false) @RequestParam(required = false) String last_id,
            @Parameter(description = "每页返回的记录数，取值范围：1-100，默认20", required = false) @RequestParam(required = false) Integer limit,
            @Parameter(description = "排序字段及顺序，默认-updated_at", required = false) @RequestParam(required = false) String sort_by) {

        try {
            // 从token中获取用户信息
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Result.error(ResultCode.UNAUTHORIZED);
            }

            User user = userMapper.selectById(userId);
            if (user == null || user.getUsername() == null) {
                return Result.error(ResultCode.UNAUTHORIZED, "用户不存在或用户名无效");
            }

            String username = user.getUsername();

            // 调用 DIFY API 获取会话列表
            Map<String, Object> difyResponse = difyClientService.getConversations(
                    username,
                    last_id,
                    limit,
                    sort_by);

            // 转换为响应 DTO
            ConversationListResponse response = new ConversationListResponse();

            if (difyResponse.containsKey("limit")) {
                response.setLimit(((Number) difyResponse.get("limit")).intValue());
            }

            if (difyResponse.containsKey("has_more")) {
                response.setHasMore((Boolean) difyResponse.get("has_more"));
            }

            if (difyResponse.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) difyResponse.get("data");
                List<ConversationItem> conversationItems = new ArrayList<>();

                if (dataList != null) {
                    for (Map<String, Object> item : dataList) {
                        ConversationItem conversationItem = new ConversationItem();
                        conversationItem.setId((String) item.get("id"));
                        conversationItem.setName((String) item.get("name"));
                        @SuppressWarnings("unchecked")
                        Map<String, Object> inputs = (Map<String, Object>) item.get("inputs");
                        conversationItem.setInputs(inputs);
                        conversationItem.setStatus((String) item.get("status"));
                        conversationItem.setIntroduction((String) item.get("introduction"));

                        if (item.get("created_at") != null) {
                            conversationItem.setCreatedAt(((Number) item.get("created_at")).longValue());
                        }

                        if (item.get("updated_at") != null) {
                            conversationItem.setUpdatedAt(((Number) item.get("updated_at")).longValue());
                        }

                        conversationItems.add(conversationItem);
                    }
                }

                response.setData(conversationItems);
            }

            return Result.success(response);

        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR,
                    "获取会话列表失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
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

    /**
     * 会话列表响应 - 完全按照 DIFY API 规范
     */
    @Schema(description = "会话列表响应，完全符合 DIFY API 规范")
    public static class ConversationListResponse {
        @Schema(description = "实际返回的条数（可能受系统限制）", example = "20")
        private Integer limit;

        @Schema(description = "是否还有更多数据可供分页", example = "false")
        private Boolean hasMore;

        @Schema(description = "会话列表")
        private List<ConversationItem> data;

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean getHasMore() {
            return hasMore;
        }

        public void setHasMore(Boolean hasMore) {
            this.hasMore = hasMore;
        }

        public List<ConversationItem> getData() {
            return data;
        }

        public void setData(List<ConversationItem> data) {
            this.data = data;
        }
    }

    /**
     * 会话项 - 完全按照 DIFY API 规范
     */
    @Schema(description = "会话项，完全符合 DIFY API 规范")
    public static class ConversationItem {
        @Schema(description = "会话唯一 ID", example = "10799fb8-64f7-4296-bbf7-b42bfbe0ae54")
        private String id;

        @Schema(description = "会话名称（通常由大语言模型自动生成）", example = "New chat")
        private String name;

        @Schema(description = "用户在该会话中传入的初始变量（键值对）")
        private Map<String, Object> inputs;

        @Schema(description = "会话状态", example = "normal")
        private String status;

        @Schema(description = "开场白（部分应用配置下存在）", example = "")
        private String introduction;

        @Schema(description = "会话创建时间（Unix 秒时间戳）", example = "1679667915")
        private Long createdAt;

        @Schema(description = "会话最后更新时间（Unix 秒时间戳）", example = "1679667915")
        private Long updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getInputs() {
            return inputs;
        }

        public void setInputs(Map<String, Object> inputs) {
            this.inputs = inputs;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getIntroduction() {
            return introduction;
        }

        public void setIntroduction(String introduction) {
            this.introduction = introduction;
        }

        public Long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }

        public Long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
