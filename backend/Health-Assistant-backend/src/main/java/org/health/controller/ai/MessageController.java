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
import org.health.common.UserContext;
import org.health.mapper.UserMapper;
import org.health.entity.User;
import org.health.service.ai.DifyClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会话历史消息控制器 - 完全按照 DIFY API 规范
 */
@Tag(name = "会话历史消息", description = "获取会话历史消息接口，完全符合 DIFY API 规范")
@RestController
@RequestMapping("/ai")
public class MessageController {

    @Autowired
    private DifyClientService difyClientService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取会话历史消息
     * GET /api/v1/ai/messages
     * 完全按照 DIFY API 规范：https://docs.dify.ai/api-reference/messages
     */
    @Operation(summary = "获取会话历史消息", description = "获取指定会话中的历史聊天记录，支持滚动加载（倒序）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回消息列表",
                    content = @Content(schema = @Schema(implementation = MessageListResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录或Token失效")
    })
    @GetMapping("/messages")
    public Result<MessageListResponse> getMessages(
            @Parameter(description = "目标会话的唯一 ID", required = true) @RequestParam(required = true) String conversation_id,
            @Parameter(description = "当前页第一条消息的 ID，用于加载更早的历史记录", required = false) @RequestParam(required = false) String first_id,
            @Parameter(description = "每次请求返回的消息数量，取值范围：1-100，默认20", required = false) @RequestParam(required = false) Integer limit) {

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

            // 调用 DIFY API 获取历史消息
            Map<String, Object> difyResponse = difyClientService.getMessages(
                    conversation_id,
                    username,
                    first_id,
                    limit);

            // 转换为响应 DTO
            MessageListResponse response = new MessageListResponse();

            if (difyResponse.containsKey("limit")) {
                response.setLimit(((Number) difyResponse.get("limit")).intValue());
            }

            if (difyResponse.containsKey("has_more")) {
                response.setHasMore((Boolean) difyResponse.get("has_more"));
            }

            if (difyResponse.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) difyResponse.get("data");
                List<MessageItem> messageItems = new ArrayList<>();

                if (dataList != null) {
                    for (Map<String, Object> item : dataList) {
                        MessageItem messageItem = new MessageItem();
                        messageItem.setId((String) item.get("id"));
                        messageItem.setConversationId((String) item.get("conversation_id"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> inputs = (Map<String, Object>) item.get("inputs");
                        messageItem.setInputs(inputs);

                        messageItem.setQuery((String) item.get("query"));
                        messageItem.setAnswer((String) item.get("answer"));

                        // 处理 message_files
                        if (item.get("message_files") != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> filesList = (List<Map<String, Object>>) item.get("message_files");
                            List<MessageFile> messageFiles = new ArrayList<>();
                            if (filesList != null) {
                                for (Map<String, Object> fileMap : filesList) {
                                    MessageFile messageFile = new MessageFile();
                                    messageFile.setId((String) fileMap.get("id"));
                                    messageFile.setType((String) fileMap.get("type"));
                                    messageFile.setUrl((String) fileMap.get("url"));
                                    messageFile.setBelongsTo((String) fileMap.get("belongs_to"));
                                    messageFiles.add(messageFile);
                                }
                            }
                            messageItem.setMessageFiles(messageFiles);
                        }

                        // 处理 feedback
                        if (item.get("feedback") != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> feedbackMap = (Map<String, Object>) item.get("feedback");
                            if (feedbackMap != null) {
                                Feedback feedback = new Feedback();
                                feedback.setRating((String) feedbackMap.get("rating"));
                                messageItem.setFeedback(feedback);
                            }
                        }

                        // 处理 retriever_resources
                        if (item.get("retriever_resources") != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> resourcesList = (List<Map<String, Object>>) item
                                    .get("retriever_resources");
                            List<RetrieverResource> retrieverResources = new ArrayList<>();
                            if (resourcesList != null) {
                                for (Map<String, Object> resourceMap : resourcesList) {
                                    RetrieverResource resource = new RetrieverResource();
                                    if (resourceMap.get("position") != null) {
                                        resource.setPosition(((Number) resourceMap.get("position")).intValue());
                                    }
                                    resource.setDatasetId((String) resourceMap.get("dataset_id"));
                                    resource.setDatasetName((String) resourceMap.get("dataset_name"));
                                    resource.setDocumentId((String) resourceMap.get("document_id"));
                                    resource.setDocumentName((String) resourceMap.get("document_name"));
                                    resource.setSegmentId((String) resourceMap.get("segment_id"));
                                    if (resourceMap.get("score") != null) {
                                        resource.setScore(((Number) resourceMap.get("score")).doubleValue());
                                    }
                                    resource.setContent((String) resourceMap.get("content"));
                                    retrieverResources.add(resource);
                                }
                            }
                            messageItem.setRetrieverResources(retrieverResources);
                        }

                        if (item.get("created_at") != null) {
                            messageItem.setCreatedAt(((Number) item.get("created_at")).longValue());
                        }

                        messageItems.add(messageItem);
                    }
                }

                response.setData(messageItems);
            }

            return Result.success(response);

        } catch (Exception e) {
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR,
                    "获取会话历史消息失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 消息列表响应 - 完全按照 DIFY API 规范
     */
    @Schema(description = "消息列表响应，完全符合 DIFY API 规范")
    public static class MessageListResponse {
        @Schema(description = "实际返回的消息条数（可能受系统限制）", example = "20")
        private Integer limit;

        @Schema(description = "是否还有更早的消息可供加载", example = "false")
        private Boolean hasMore;

        @Schema(description = "消息列表（按时间倒序排列，最新在前）")
        private List<MessageItem> data;

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

        public List<MessageItem> getData() {
            return data;
        }

        public void setData(List<MessageItem> data) {
            this.data = data;
        }
    }

    /**
     * 消息项 - 完全按照 DIFY API 规范
     */
    @Schema(description = "消息项，完全符合 DIFY API 规范")
    public static class MessageItem {
        @Schema(description = "消息唯一 ID", example = "a076a87f-31e5-48dc-b452-0061adbbc922")
        private String id;

        @Schema(description = "所属会话 ID", example = "cd78daf6-f9e4-4463-9ff2-54257230a0ce")
        private String conversationId;

        @Schema(description = "用户在该轮对话中传入的变量（键值对）")
        private Map<String, Object> inputs;

        @Schema(description = "用户输入/提问内容", example = "iphone 13 pro")
        private String query;

        @Schema(description = "助手回复内容", example = "The iPhone 13 Pro...")
        private String answer;

        @Schema(description = "本轮消息关联的文件列表")
        private List<MessageFile> messageFiles;

        @Schema(description = "用户反馈信息")
        private Feedback feedback;

        @Schema(description = "引用的知识库片段（如有）")
        private List<RetrieverResource> retrieverResources;

        @Schema(description = "消息创建时间（Unix 秒时间戳）", example = "1705569239")
        private Long createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public List<MessageFile> getMessageFiles() {
            return messageFiles;
        }

        public void setMessageFiles(List<MessageFile> messageFiles) {
            this.messageFiles = messageFiles;
        }

        public Feedback getFeedback() {
            return feedback;
        }

        public void setFeedback(Feedback feedback) {
            this.feedback = feedback;
        }

        public List<RetrieverResource> getRetrieverResources() {
            return retrieverResources;
        }

        public void setRetrieverResources(List<RetrieverResource> retrieverResources) {
            this.retrieverResources = retrieverResources;
        }

        public Long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 消息文件 - 完全按照 DIFY API 规范
     */
    @Schema(description = "消息文件，完全符合 DIFY API 规范")
    public static class MessageFile {
        @Schema(description = "文件唯一 ID", example = "file-123")
        private String id;

        @Schema(description = "文件类型（目前主要为 image）", example = "image")
        private String type;

        @Schema(description = "文件预览地址", example = "https://...")
        private String url;

        @Schema(description = "文件归属方：user 或 assistant", example = "user")
        private String belongsTo;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBelongsTo() {
            return belongsTo;
        }

        public void setBelongsTo(String belongsTo) {
            this.belongsTo = belongsTo;
        }
    }

    /**
     * 用户反馈 - 完全按照 DIFY API 规范
     */
    @Schema(description = "用户反馈，完全符合 DIFY API 规范")
    public static class Feedback {
        @Schema(description = "用户反馈：like 表示点赞，dislike 表示点踩", example = "like", allowableValues = { "like", "dislike" })
        private String rating;

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }
    }

    /**
     * 检索资源 - 完全按照 DIFY API 规范
     */
    @Schema(description = "检索资源，完全符合 DIFY API 规范")
    public static class RetrieverResource {
        @Schema(description = "引用顺序", example = "1")
        private Integer position;

        @Schema(description = "数据集 ID", example = "101b4c97-fc2e-463c-90b1-5261a4cdcafb")
        private String datasetId;

        @Schema(description = "数据集名称", example = "iPhone")
        private String datasetName;

        @Schema(description = "文档 ID", example = "8dd1ad74-0b5f-4175-b735-7d98bbbb4e00")
        private String documentId;

        @Schema(description = "文档名称", example = "iPhone List")
        private String documentName;

        @Schema(description = "片段 ID", example = "ed599c7f-2766-4294-9d1d-e5235a61270a")
        private String segmentId;

        @Schema(description = "相似度得分", example = "0.98457545")
        private Double score;

        @Schema(description = "原始文本内容", example = "\"Model\",\"Release Date\",...\"iOS 15\"")
        private String content;

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public String getSegmentId() {
            return segmentId;
        }

        public void setSegmentId(String segmentId) {
            this.segmentId = segmentId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

