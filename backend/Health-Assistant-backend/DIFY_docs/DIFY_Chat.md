# Dify 发送对话消息 API 文档

## 概述

该接口用于向 Dify 应用发送用户消息，创建会话并获取模型响应。支持**流式（streaming）**与**阻塞（blocking）**两种响应模式，并可传入上下文变量、文件、指定会话 ID 等。

---

## 请求信息

- **方法**：`POST`
- **路径**：`/v1/chat-messages`
- **认证**：需在 Header 中提供 `Authorization: Bearer {api_key}`

### 请求示例（cURL）

```bash
curl -X POST 'https://api.dify.ai/v1/chat-messages' \
  --header 'Authorization: Bearer {api_key}' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "inputs": {},
    "query": "What are the specs of the iPhone 13 Pro Max?",
    "response_mode": "streaming",
    "conversation_id": "",
    "user": "abc-123",
    "files": [
      {
        "type": "image",
        "transfer_method": "remote_url",
        "url": "https://cloud.dify.ai/logo/logo-site.png"
      }
    ]
  }'
```

---

## 请求体参数（Request Body）

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `query` | `string` | ✅ | 用户输入/提问内容。 |
| `inputs` | `object` | ❌ | App 定义的变量值，键值对形式。若变量为文件类型，则值应为符合 `files` 结构的对象。默认 `{}`。 |
| `response_mode` | `string` | ✅ | 响应模式：<br>• `streaming`（推荐）：基于 SSE 流式返回。<br>• `blocking`：等待执行完成一次性返回（最长 100 秒，受 Cloudflare 限制）。 |
| `user` | `string` | ✅ | 用户唯一标识，由开发者定义，用于追踪与统计。 |
| `conversation_id` | `string` | ❌ | 若需延续历史对话，必须提供此前返回的 `conversation_id`。 |
| `files` | `array[object]` | ❌ | 文件列表，仅当模型支持 Vision/Video 能力时可用。 |

### `files` 数组项结构

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `type` | `string` | ✅ | 文件类型：<br>• `document`: TXT, MD, PDF, DOCX, CSV 等<br>• `image`: JPG, PNG, WEBP 等<br>• `audio`: MP3, WAV, WEBM 等<br>• `video`: MP4, MOV, WEBM 等<br>• `custom`: 其他类型 |
| `transfer_method` | `string` | ✅ | 传递方式：<br>• `remote_url`：通过 URL 引用<br>• `local_file`：通过已上传文件 ID 引用 |
| `url` | `string` | ⚠️ | 仅当 `transfer_method = remote_url` 时必填，文件公网可访问地址。 |
| `upload_file_id` | `string` | ⚠️ | 仅当 `transfer_method = local_file` 时必填，已上传文件的 ID。 |

### 其他可选参数

| 参数名 | 类型 | 描述 |
|--------|------|------|
| `auto_generate_name` | `bool` | 是否自动生成会话标题，默认 `true`。设为 `false` 可后续调用重命名接口异步生成。 |
| `workflow_id` | `string` | 指定使用的工作流版本 ID（UUID 格式），若不提供则使用默认已发布版本。 |
| `trace_id` | `string` | 链路追踪 ID，用于端到端分布式追踪。优先级：<br>1. HTTP Header `X-Trace-Id`<br>2. URL Query 参数 `trace_id`<br>3. Request Body 中的 `trace_id` 字段 |

---

## 响应（Response）

根据 `response_mode` 不同，返回格式不同：

### 1. 阻塞模式（`blocking`）

- **Content-Type**: `application/json`
- **返回对象**: `ChatCompletionResponse`

#### 字段说明

| 字段 | 类型 | 描述 |
|------|------|------|
| `event` | `string` | 固定为 `"message"` |
| `task_id` | `string` | 任务 ID，用于跟踪或停止请求 |
| `id` / `message_id` | `string` | 消息唯一 ID |
| `conversation_id` | `string` | 会话 ID |
| `mode` | `string` | 固定为 `"chat"` |
| `answer` | `string` | 完整回复内容 |
| `metadata` | `object` | 元数据，含用量与引用资源 |
| `created_at` | `int` | 消息创建时间戳（Unix 秒） |

##### `metadata` 子字段

- `usage`: 模型用量（见下表）
- `retriever_resources`: 引用的知识库片段列表

###### `usage` 结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `prompt_tokens` | `int` | 输入 token 数 |
| `completion_tokens` | `int` | 输出 token 数 |
| `total_tokens` | `int` | 总 token 数 |
| `total_price` | `string` | 总费用（字符串格式） |
| `currency` | `string` | 货币单位（如 USD） |
| `latency` | `float` | 响应延迟（秒） |

###### `retriever_resources` 项结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `position` | `int` | 引用顺序 |
| `dataset_id` / `dataset_name` | `string` | 数据集信息 |
| `document_id` / `document_name` | `string` | 文档信息 |
| `segment_id` | `string` | 片段 ID |
| `score` | `float` | 相似度得分 |
| `content` | `string` | 原始文本内容 |

---

### 2. 流式模式（`streaming`）

- **Content-Type**: `text/event-stream`
- **格式**: 每个事件以 `data: {JSON}\n\n` 形式输出
- **事件类型**多样，按 `event` 字段区分

#### 支持的事件类型

##### `event: message`
LLM 返回文本块。

| 字段 | 类型 | 说明 |
|------|------|------|
| `task_id`, `message_id`, `conversation_id` | `string` | 上下文标识 |
| `answer` | `string` | 当前文本块内容 |
| `created_at` | `int` | 时间戳 |

##### `event: message_file`
返回生成的文件（目前仅支持 image）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `string` | 文件唯一 ID |
| `type` | `string` | 文件类型（如 `"image"`） |
| `belongs_to` | `string` | 归属方（固定为 `"assistant"`） |
| `url` | `string` | 可访问的文件地址 |
| `conversation_id` | `string` | 会话 ID |

##### `event: message_end`
消息结束事件，包含完整元数据。

| 字段 | 类型 | 说明 |
|------|------|------|
| `metadata` | `object` | 含 `usage` 和 `retriever_resources`（同阻塞模式） |

##### `event: tts_message` / `tts_message_end`
TTS 语音合成音频流（Base64 编码 MP3）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `audio` | `string` | Base64 编码的音频块（`tts_message_end` 中为空） |

##### `event: message_replace`
内容审查触发，替换原回答。

| 字段 | 类型 | 说明 |
|------|------|------|
| `answer` | `string` | 审查后的预设回复 |

##### Workflow 相关事件（适用于工作流应用）

- `workflow_started`
- `node_started`
- `node_finished`
- `workflow_finished`

> 包含执行 ID、节点信息、状态（succeeded/failed）、耗时、输入输出、token 用量等。

##### `event: error`
流式过程中发生错误。

| 字段 | 类型 | 说明 |
|------|------|------|
| `status` | `int` | HTTP 状态码 |
| `code` | `string` | 错误码 |
| `message` | `string` | 错误描述 |

##### `event: ping`
每 10 秒一次的心跳事件，保持连接。

---

## 错误码（Errors）

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 404 | - | 对话不存在 |
| 400 | `invalid_param` | 参数格式错误 |
| 400 | `app_unavailable` | App 配置不可用 |
| 400 | `provider_not_initialize` | 未配置模型凭据 |
| 400 | `provider_quota_exceeded` | 模型调用额度不足 |
| 400 | `model_currently_not_support` | 当前模型不可用 |
| 400 | `workflow_not_found` | 指定工作流版本不存在 |
| 400 | `draft_workflow_error` | 无法使用草稿版工作流 |
| 400 | `workflow_id_format_error` | `workflow_id` 非 UUID 格式 |
| 400 | `completion_request_error` | 文本生成失败 |
| 500 | - | 服务内部异常 |

---

## 附录：获取对话变量（GET `/conversations/:conversation_id/variables`）

用于从特定对话中提取结构化变量。

### 请求参数

- **路径参数**
  - `conversation_id`: 对话 ID

- **查询参数**
  - `user` (必填): 用户标识
  - `last_id` (可选): 分页游标
  - `limit` (可选): 每页数量（1–100，默认 20）

### 响应结构

```json
{
  "limit": 100,
  "has_more": false,
  "data": [
    {
      "id": "variable-uuid-1",
      "name": "customer_name",
      "value_type": "string",
      "value": "John Doe",
      "description": "客户名称（从对话中提取）",
      "created_at": 1650000000,
      "updated_at": 1650000000
    }
  ]
}
```

### 错误

- `404 conversation_not_exists`: 对话不存在

--- 

> 💡 提示：建议优先使用 `streaming` 模式以获得更佳用户体验和更低超时风险。