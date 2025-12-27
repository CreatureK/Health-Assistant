package org.health.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DIFY API 客户端服务
 */
@Service
public class DifyClientService {

  @Value("${DIFY_BASE_URL}")
  private String baseUrl;

  @Value("${DIFY_API_KEY}")
  private String apiKey;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public DifyClientService() {
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 发送流式聊天消息
   *
   * @param query          用户消息
   * @param conversationId 会话ID（可选，为空则创建新会话）
   * @param user           用户标识
   * @param emitter        流式响应发射器
   * @return conversationId 会话ID
   */
  public String streamChat(String query, String conversationId, String user, SseEmitter emitter) {
    try {
      String url = baseUrl + "/v1/chat-messages";

      // 构建请求体（完全符合 DIFY API 文档要求）
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("query", query); // 必填：用户输入/提问内容
      requestBody.put("response_mode", "streaming"); // 必填：响应模式
      requestBody.put("user", user); // 必填：用户唯一标识
      requestBody.put("inputs", new HashMap<>()); // 可选：App 定义的变量值，默认 {}
      // conversation_id 可选：为空字符串时表示创建新会话，否则延续历史对话
      requestBody.put("conversation_id", (conversationId != null && !conversationId.isEmpty()) ? conversationId : "");

      // 创建 HTTP 连接
      URL apiUrl = new java.net.URI(url).toURL();
      HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "text/event-stream");
      connection.setDoOutput(true);
      connection.setDoInput(true);

      // 发送请求体
      String jsonBody = objectMapper.writeValueAsString(requestBody);
      connection.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));

      // 检查响应状态
      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        BufferedReader errorReader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder errorResponse = new StringBuilder();
        String line;
        while ((line = errorReader.readLine()) != null) {
          errorResponse.append(line);
        }
        errorReader.close();
        throw new RuntimeException("DIFY API 调用失败: " + responseCode + " - " + errorResponse.toString());
      }

      // 读取流式响应
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

      String currentConversationId = null;
      boolean conversationIdSent = false;
      StringBuilder fullAnswer = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        if (line.startsWith("data: ")) {
          String jsonData = line.substring(6).trim();
          if (jsonData.isEmpty() || "null".equals(jsonData)) {
            continue;
          }

          try {
            JsonNode eventNode = objectMapper.readTree(jsonData);
            String event = eventNode.has("event") ? eventNode.get("event").asText() : "";

            // 处理 message 事件（文本块）
            if ("message".equals(event)) {
              // 首次收到 conversation_id 时立即发送
              if (eventNode.has("conversation_id") && !conversationIdSent) {
                currentConversationId = eventNode.get("conversation_id").asText();
                Map<String, Object> convData = new HashMap<>();
                convData.put("event", "conversation_id");
                convData.put("conversation_id", currentConversationId);
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(convData)));
                conversationIdSent = true;
              }

              if (eventNode.has("answer")) {
                String answerChunk = eventNode.get("answer").asText();
                fullAnswer.append(answerChunk);

                // 发送文本块给客户端
                Map<String, Object> chunkData = new HashMap<>();
                chunkData.put("event", "message");
                chunkData.put("answer", answerChunk);
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunkData)));
              }

              // 更新 conversation_id（如果后续事件中有更新）
              if (eventNode.has("conversation_id")) {
                currentConversationId = eventNode.get("conversation_id").asText();
              }
            }
            // 处理 message_end 事件
            else if ("message_end".equals(event)) {
              if (eventNode.has("conversation_id")) {
                currentConversationId = eventNode.get("conversation_id").asText();
              }

              // 发送结束事件
              Map<String, Object> endData = new HashMap<>();
              endData.put("event", "message_end");
              if (eventNode.has("metadata")) {
                endData.put("metadata", eventNode.get("metadata"));
              }
              emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(endData)));
              break;
            }
            // 处理 error 事件
            else if ("error".equals(event)) {
              String errorMessage = eventNode.has("message")
                  ? eventNode.get("message").asText()
                  : "未知错误";
              throw new RuntimeException("DIFY API 错误: " + errorMessage);
            }
            // 忽略 ping 事件
            else if ("ping".equals(event)) {
              continue;
            }
          } catch (Exception e) {
            // JSON 解析错误，继续处理下一行
            continue;
          }
        }
      }

      reader.close();
      connection.disconnect();

      return currentConversationId != null ? currentConversationId : conversationId;
    } catch (Exception e) {
      try {
        emitter.send(SseEmitter.event()
            .data(objectMapper.writeValueAsString(Map.of("event", "error", "message", e.getMessage()))));
        emitter.completeWithError(e);
      } catch (Exception ex) {
        // 忽略发送错误
      }
      throw new RuntimeException("流式聊天请求失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取会话列表
   *
   * @param user 用户标识
   * @return 会话列表响应
   */
  public ConversationsResponse getConversations(String user) {
    try {
      String url = baseUrl + "/v1/conversations?user=" + user + "&limit=100";

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + apiKey);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        ConversationsResponse result = new ConversationsResponse();
        result.setLimit(rootNode.has("limit") ? rootNode.get("limit").asInt() : 0);
        result.setHasMore(rootNode.has("has_more") ? rootNode.get("has_more").asBoolean() : false);

        List<ConversationVO> conversations = new ArrayList<>();
        if (rootNode.has("data") && rootNode.get("data").isArray()) {
          for (JsonNode item : rootNode.get("data")) {
            ConversationVO vo = new ConversationVO();
            vo.setId(item.has("id") ? item.get("id").asText() : null);
            vo.setName(item.has("name") ? item.get("name").asText() : null);
            vo.setStatus(item.has("status") ? item.get("status").asText() : null);
            vo.setIntroduction(item.has("introduction") ? item.get("introduction").asText() : null);

            if (item.has("created_at")) {
              long timestamp = item.get("created_at").asLong();
              vo.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
            }
            if (item.has("updated_at")) {
              long timestamp = item.get("updated_at").asLong();
              vo.setUpdatedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
            }

            conversations.add(vo);
          }
        }
        result.setData(conversations);
        return result;
      } else {
        throw new RuntimeException("获取会话列表失败: " + response.getStatusCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("获取会话列表失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取会话消息历史
   *
   * @param conversationId 会话ID
   * @param user           用户标识
   * @return 消息列表响应
   */
  public MessagesResponse getMessages(String conversationId, String user) {
    try {
      String url = baseUrl + "/v1/messages?conversation_id=" + conversationId + "&user=" + user + "&limit=100";

      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bearer " + apiKey);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        MessagesResponse result = new MessagesResponse();
        result.setLimit(rootNode.has("limit") ? rootNode.get("limit").asInt() : 0);
        result.setHasMore(rootNode.has("has_more") ? rootNode.get("has_more").asBoolean() : false);

        List<MessageVO> messages = new ArrayList<>();
        if (rootNode.has("data") && rootNode.get("data").isArray()) {
          for (JsonNode item : rootNode.get("data")) {
            // 用户消息
            if (item.has("query") && !item.get("query").asText().isEmpty()) {
              MessageVO userMsg = new MessageVO();
              userMsg.setRole("user");
              userMsg.setContent(item.get("query").asText());
              userMsg.setInputType("text");
              if (item.has("created_at")) {
                long timestamp = item.get("created_at").asLong();
                userMsg.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
              }
              messages.add(userMsg);
            }

            // AI回复消息
            if (item.has("answer") && !item.get("answer").asText().isEmpty()) {
              MessageVO assistantMsg = new MessageVO();
              assistantMsg.setRole("assistant");
              assistantMsg.setContent(item.get("answer").asText());
              assistantMsg.setInputType("text");
              if (item.has("created_at")) {
                long timestamp = item.get("created_at").asLong();
                assistantMsg
                    .setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
              }
              messages.add(assistantMsg);
            }
          }
        }
        result.setData(messages);
        return result;
      } else {
        throw new RuntimeException("获取消息历史失败: " + response.getStatusCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("获取消息历史失败: " + e.getMessage(), e);
    }
  }

  /**
   * 会话列表响应
   */
  public static class ConversationsResponse {
    private int limit;
    private boolean hasMore;
    private List<ConversationVO> data;

    public int getLimit() {
      return limit;
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }

    public boolean isHasMore() {
      return hasMore;
    }

    public void setHasMore(boolean hasMore) {
      this.hasMore = hasMore;
    }

    public List<ConversationVO> getData() {
      return data;
    }

    public void setData(List<ConversationVO> data) {
      this.data = data;
    }
  }

  /**
   * 会话视图对象
   */
  public static class ConversationVO {
    private String id;
    private String name;
    private String status;
    private String introduction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
   * 消息列表响应
   */
  public static class MessagesResponse {
    private int limit;
    private boolean hasMore;
    private List<MessageVO> data;

    public int getLimit() {
      return limit;
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }

    public boolean isHasMore() {
      return hasMore;
    }

    public void setHasMore(boolean hasMore) {
      this.hasMore = hasMore;
    }

    public List<MessageVO> getData() {
      return data;
    }

    public void setData(List<MessageVO> data) {
      this.data = data;
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
