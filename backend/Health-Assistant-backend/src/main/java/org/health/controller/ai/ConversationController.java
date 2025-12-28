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
 * 会话列表控制器 - 完全按照 DIFY API 规范
 */
@Tag(name = "会话列表", description = "获取会话列表接口，完全符合 DIFY API 规范")
@RestController
@RequestMapping("/ai")
public class ConversationController {

  @Autowired
  private DifyClientService difyClientService;

  @Autowired
  private UserMapper userMapper;

  /**
   * 获取会话列表
   * GET /api/v1/ai/conversations
   * 完全按照 DIFY API 规范：https://docs.dify.ai/api-reference/conversations
   */
  @Operation(summary = "获取会话列表", description = "获取指定用户的会话列表，支持分页与排序")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "成功返回会话列表", content = @Content(schema = @Schema(implementation = ConversationListResponse.class))),
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
