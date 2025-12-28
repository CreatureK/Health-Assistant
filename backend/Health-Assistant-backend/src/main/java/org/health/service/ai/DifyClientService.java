package org.health.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DIFY API 客户端服务
 */
@Service
public class DifyClientService {

  private static final Logger logger = LoggerFactory.getLogger(DifyClientService.class);

  @Value("${DIFY_BASE_URL}")
  private String baseUrl;

  @Value("${DIFY_API_KEY}")
  private String apiKey;

  private final ObjectMapper objectMapper;

  public DifyClientService() {
    this.objectMapper = new ObjectMapper();
  }

  @PostConstruct
  public void init() {
    // 验证配置是否正确加载
    if (baseUrl == null || baseUrl.isEmpty()) {
      logger.error("DIFY_BASE_URL 未配置！");
    } else {
      logger.info("DIFY_BASE_URL 已配置: {}", baseUrl);
    }

    if (apiKey == null || apiKey.isEmpty()) {
      logger.error("DIFY_API_KEY 未配置！");
    } else {
      // 只显示前10个字符，避免泄露完整密钥
      String maskedKey = apiKey.length() > 10
          ? apiKey.substring(0, 10) + "..."
          : "***";
      logger.info("DIFY_API_KEY 已配置: {}", maskedKey);
    }
  }

  /**
   * 发送流式聊天消息 - 完全按照 DIFY API 规范
   *
   * @param query            用户消息（必填）
   * @param conversationId   会话ID（可选）
   * @param user             用户标识（必填）
   * @param inputs           App定义的变量值（可选）
   * @param autoGenerateName 是否自动生成会话标题（可选）
   * @param emitter          流式响应发射器
   * @return conversationId 会话ID
   */
  public String streamChat(
      String query,
      String conversationId,
      String user,
      Map<String, Object> inputs,
      Boolean autoGenerateName,
      SseEmitter emitter) {
    try {
      String url = baseUrl + "/v1/chat-messages";

      // 构建请求体（完全符合 DIFY API 文档要求）
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("query", query); // 必填
      requestBody.put("response_mode", "streaming"); // 必填
      requestBody.put("user", user); // 必填

      // 可选参数
      if (conversationId != null && !conversationId.isEmpty()) {
        requestBody.put("conversation_id", conversationId);
      } else {
        requestBody.put("conversation_id", "");
      }

      requestBody.put("inputs", inputs != null ? inputs : new HashMap<>());

      if (autoGenerateName != null) {
        requestBody.put("auto_generate_name", autoGenerateName);
      }

      // 验证 apiKey 是否已正确配置
      if (apiKey == null || apiKey.isEmpty()) {
        throw new RuntimeException("DIFY_API_KEY 未配置，请检查环境变量或配置文件");
      }

      // 创建 HTTP 连接
      URL apiUrl = new java.net.URI(url).toURL();
      HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
      connection.setRequestMethod("POST");
      // 使用 DIFY_API_KEY，而不是用户登录的 token
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "text/event-stream");
      connection.setDoOutput(true);
      connection.setDoInput(true);

      logger.debug("向 DIFY 发送请求: URL={}, Authorization=Bearer {}...", url,
          apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "***");

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
        connection.disconnect();

        // 发送错误消息并完成 emitter
        String errorMessage = "DIFY API 调用失败: " + responseCode + " - " + errorResponse.toString();
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("event", "error");
        errorData.put("status", responseCode);
        errorData.put("code", "api_error");
        errorData.put("message", errorMessage);

        try {
          emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(errorData)));
          emitter.completeWithError(new RuntimeException(errorMessage));
        } catch (Exception ex) {
          try {
            emitter.completeWithError(new RuntimeException(errorMessage));
          } catch (Exception ignored) {
          }
        }
        throw new EmitterAlreadyCompletedException(errorMessage);
      }

      // 读取流式响应并透传所有事件
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

      String currentConversationId = conversationId;
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

            // 更新 conversation_id
            if (eventNode.has("conversation_id")) {
              currentConversationId = eventNode.get("conversation_id").asText();
            }

            // 透传所有事件类型给客户端
            // 包括：message, message_file, message_end, tts_message, tts_message_end,
            // message_replace, error, ping, workflow_started, node_started,
            // node_finished, workflow_finished 等
            emitter.send(SseEmitter.event().data(jsonData));

            // message_end 事件后结束
            if ("message_end".equals(event)) {
              break;
            }

            // error 事件后抛出异常
            if ("error".equals(event)) {
              String errorMsg = eventNode.has("message")
                  ? eventNode.get("message").asText()
                  : "未知错误";
              throw new RuntimeException("DIFY API 错误: " + errorMsg);
            }

          } catch (RuntimeException e) {
            // 重新抛出业务异常
            throw e;
          } catch (Exception e) {
            // JSON 解析错误，继续处理下一行
            continue;
          }
        }
      }

      reader.close();
      connection.disconnect();

      return currentConversationId != null ? currentConversationId : conversationId;

    } catch (EmitterAlreadyCompletedException e) {
      throw e;
    } catch (Exception e) {
      // 发送错误消息并完成 emitter
      Map<String, Object> errorData = new HashMap<>();
      errorData.put("event", "error");
      errorData.put("status", 500);
      errorData.put("code", "internal_error");
      errorData.put("message", e.getMessage());

      try {
        emitter.send(SseEmitter.event()
            .data(objectMapper.writeValueAsString(errorData)));
        emitter.completeWithError(e);
      } catch (Exception ex) {
        try {
          emitter.completeWithError(e);
        } catch (Exception ignored) {
        }
      }
      throw new EmitterAlreadyCompletedException("流式聊天请求失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取会话列表 - 完全按照 DIFY API 规范
   *
   * @param user   用户唯一标识（必填）
   * @param lastId 分页游标：当前页最后一条记录的 id（可选）
   * @param limit  每页返回的记录数，取值范围：1-100，默认20（可选）
   * @param sortBy 排序字段及顺序，默认"-updated_at"（可选）
   * @return 会话列表响应，包含 limit、has_more、data 字段
   * @throws RuntimeException 当 API 调用失败时抛出
   */
  public Map<String, Object> getConversations(
      String user,
      String lastId,
      Integer limit,
      String sortBy) {
    try {
      // 构建查询参数
      StringBuilder queryParams = new StringBuilder();
      queryParams.append("user=").append(URLEncoder.encode(user, StandardCharsets.UTF_8));

      if (lastId != null && !lastId.isEmpty()) {
        queryParams.append("&last_id=").append(URLEncoder.encode(lastId, StandardCharsets.UTF_8));
      }

      if (limit != null && limit > 0) {
        // 限制在 1-100 范围内
        int actualLimit = Math.min(Math.max(limit, 1), 100);
        queryParams.append("&limit=").append(actualLimit);
      } else {
        queryParams.append("&limit=20");
      }

      if (sortBy != null && !sortBy.isEmpty()) {
        queryParams.append("&sort_by=").append(URLEncoder.encode(sortBy, StandardCharsets.UTF_8));
      } else {
        queryParams.append("&sort_by=-updated_at");
      }

      String url = baseUrl + "/v1/conversations?" + queryParams.toString();

      // 验证 apiKey 是否已正确配置
      if (apiKey == null || apiKey.isEmpty()) {
        throw new RuntimeException("DIFY_API_KEY 未配置，请检查环境变量或配置文件");
      }

      // 创建 HTTP 连接
      URL apiUrl = new java.net.URI(url).toURL();
      HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Accept", "application/json");
      connection.setDoInput(true);

      logger.debug("向 DIFY 发送请求: URL={}, Authorization=Bearer {}...", url,
          apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "***");

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
        connection.disconnect();

        String errorMessage = "DIFY API 调用失败: " + responseCode + " - " + errorResponse.toString();
        logger.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }

      // 读取响应
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();
      connection.disconnect();

      // 解析 JSON 响应
      String jsonResponse = response.toString();
      Map<String, Object> result = objectMapper.readValue(jsonResponse,
          objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

      logger.debug("DIFY API 响应: {}", jsonResponse);
      return result;

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      String errorMessage = "获取会话列表失败: " + e.getMessage();
      logger.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }
}
