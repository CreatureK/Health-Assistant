package org.health.service.ai;

import org.health.common.UserContext;
import org.health.entity.ai.AiMessage;
import org.health.entity.ai.AiSession;
import org.health.mapper.ai.AiMessageMapper;
import org.health.mapper.ai.AiSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天服务
 */
@Service
public class AiChatService {

  @Autowired
  private AiSessionMapper aiSessionMapper;

  @Autowired
  private AiMessageMapper aiMessageMapper;

  /**
   * 发送消息
   *
   * @param request 聊天请求
   * @return 聊天响应
   */
  public ChatResponse chat(ChatRequest request) {
    Long userId = UserContext.getUserId();

    // TODO: 具体业务逻辑待实现
    // 1. 如果sessionId为空，创建新会话
    // 2. 保存用户消息
    // 3. 调用AI接口获取回复
    // 4. 保存AI回复
    // 5. 获取最近3轮消息（6条消息，3对）
    // 6. 返回响应

    ChatResponse response = new ChatResponse();
    response.setSessionId(1L);
    response.setReplyText("AI回复功能待实现");
    response.setSafetyHint(null);

    // TODO: 获取最近3轮消息（6条消息）
    // List<AiMessage> recentMessages =
    // aiMessageMapper.selectRecentBySessionId(sessionId, 6);
    // 转换为MessageVO列表
    response.setMessages(new ArrayList<>());

    return response;
  }

  /**
   * 获取会话列表
   *
   * @return 会话列表
   */
  public List<SessionVO> getSessions() {
    Long userId = UserContext.getUserId();

    // TODO: 具体业务逻辑待实现
    List<AiSession> sessions = aiSessionMapper.selectByUserId(userId);

    return sessions.stream().map(session -> {
      SessionVO vo = new SessionVO();
      vo.setId(session.getId());
      vo.setTitle(session.getTitle());
      vo.setCreatedAt(session.getCreatedAt());
      vo.setUpdatedAt(session.getUpdatedAt());
      return vo;
    }).collect(Collectors.toList());
  }

  /**
   * 获取会话消息列表
   *
   * @param sessionId 会话ID
   * @return 消息列表
   */
  public List<MessageVO> getMessages(Long sessionId) {
    Long userId = UserContext.getUserId();

    // TODO: 验证会话是否属于当前用户
    // AiSession session = aiSessionMapper.selectById(sessionId);
    // if (session == null || !session.getUserId().equals(userId)) {
    // throw new RuntimeException("会话不存在或无权限");
    // }

    // TODO: 具体业务逻辑待实现
    List<AiMessage> messages = aiMessageMapper.selectBySessionId(sessionId);

    return messages.stream().map(message -> {
      MessageVO vo = new MessageVO();
      vo.setRole(message.getRole());
      vo.setContent(message.getContent());
      vo.setInputType(message.getInputType());
      vo.setSafetyHint(message.getSafetyHint());
      vo.setCreatedAt(message.getCreatedAt());
      return vo;
    }).collect(Collectors.toList());
  }

  /**
   * 聊天请求
   */
  public static class ChatRequest {
    private Long sessionId;
    private String message;
    private String inputType;

    public Long getSessionId() {
      return sessionId;
    }

    public void setSessionId(Long sessionId) {
      this.sessionId = sessionId;
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
   * 聊天响应
   */
  public static class ChatResponse {
    private Long sessionId;
    private String replyText;
    private String safetyHint;
    private List<MessageVO> messages;

    public Long getSessionId() {
      return sessionId;
    }

    public void setSessionId(Long sessionId) {
      this.sessionId = sessionId;
    }

    public String getReplyText() {
      return replyText;
    }

    public void setReplyText(String replyText) {
      this.replyText = replyText;
    }

    public String getSafetyHint() {
      return safetyHint;
    }

    public void setSafetyHint(String safetyHint) {
      this.safetyHint = safetyHint;
    }

    public List<MessageVO> getMessages() {
      return messages;
    }

    public void setMessages(List<MessageVO> messages) {
      this.messages = messages;
    }
  }

  /**
   * 会话视图对象
   */
  public static class SessionVO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
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
