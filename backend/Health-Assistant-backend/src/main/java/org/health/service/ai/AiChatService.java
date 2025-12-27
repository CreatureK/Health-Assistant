package org.health.service.ai;

import org.health.common.UserContext;
import org.health.mapper.UserMapper;
import org.health.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天服务
 */
@Service
public class AiChatService {

  @Autowired
  private DifyClientService difyClientService;

  @Autowired
  private UserMapper userMapper;

  /**
   * 发送流式消息
   *
   * @param request 聊天请求
   * @param emitter 流式响应发射器
   * @return conversationId 会话ID
   */
  public String streamChat(ChatRequest request, SseEmitter emitter) {
    Long userId = UserContext.getUserId();

    // 获取用户信息以获取 username
    User user = userMapper.selectById(userId);
    if (user == null || user.getUsername() == null) {
      throw new RuntimeException("用户不存在或用户名无效");
    }

    String username = user.getUsername();
    String conversationId = request.getConversationId();

    // 调用 DIFY API 发送流式消息
    String resultConversationId = difyClientService.streamChat(
        request.getMessage(),
        conversationId,
        username,
        emitter);

    return resultConversationId;
  }

  /**
   * 获取会话列表
   *
   * @return 会话列表
   */
  public List<SessionVO> getSessions() {
    Long userId = UserContext.getUserId();

    // 获取用户信息以获取 username
    User user = userMapper.selectById(userId);
    if (user == null || user.getUsername() == null) {
      throw new RuntimeException("用户不存在或用户名无效");
    }

    String username = user.getUsername();

    // 从 DIFY 获取会话列表
    DifyClientService.ConversationsResponse response = difyClientService.getConversations(username);

    return response.getData().stream().map(conv -> {
      SessionVO vo = new SessionVO();
      vo.setId(conv.getId());
      vo.setTitle(conv.getName() != null ? conv.getName() : "新对话");
      vo.setCreatedAt(conv.getCreatedAt());
      vo.setUpdatedAt(conv.getUpdatedAt());
      return vo;
    }).collect(Collectors.toList());
  }

  /**
   * 获取会话消息列表
   *
   * @param conversationId 会话ID（DIFY的conversation_id）
   * @return 消息列表
   */
  public List<MessageVO> getMessages(String conversationId) {
    Long userId = UserContext.getUserId();

    // 获取用户信息以获取 username
    User user = userMapper.selectById(userId);
    if (user == null || user.getUsername() == null) {
      throw new RuntimeException("用户不存在或用户名无效");
    }

    String username = user.getUsername();

    // 从 DIFY 获取消息历史
    DifyClientService.MessagesResponse response = difyClientService.getMessages(conversationId, username);

    return response.getData().stream().map(msg -> {
      MessageVO vo = new MessageVO();
      vo.setRole(msg.getRole());
      vo.setContent(msg.getContent());
      vo.setInputType(msg.getInputType() != null ? msg.getInputType() : "text");
      vo.setSafetyHint(msg.getSafetyHint());
      vo.setCreatedAt(msg.getCreatedAt());
      return vo;
    }).collect(Collectors.toList());
  }

  /**
   * 聊天请求
   */
  public static class ChatRequest {
    private String conversationId;
    private String message;
    private String inputType;

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
  public static class SessionVO {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  /**
   * 消息视图对象
   */
  public static class MessageVO {
    private String role;
    private String content;
    private String inputType;
    private String safetyHint;
    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }
}
