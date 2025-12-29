# Dify 获取会话列表 API 文档

## 概述

该接口用于获取指定用户的会话（对话）列表，默认返回最近的 20 条记录，支持分页与排序。

---

## 请求信息

- **方法**：`GET`
- **路径**：`/v1/conversations`
- **认证**：需在 Header 中提供 `Authorization: Bearer {api_key}`

### 请求示例（cURL）

```bash
curl -X GET 'https://api.dify.ai/v1/conversations?user=abc-123&last_id=&limit=20' \
  --header 'Authorization: Bearer {api_key}'
```

---

## 查询参数（Query Parameters）

| 参数名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| `user` | `string` | ✅ | — | 用户唯一标识，由开发者定义，需在应用内保证唯一性。 |
| `last_id` | `string` | ❌ | `null` | 分页游标：当前页最后一条记录的 `id`，用于获取下一页数据。 |
| `limit` | `int` | ❌ | `20` | 每页返回的记录数，取值范围：`1` ~ `100`。若超出范围，将自动调整为系统允许的最大值。 |
| `sort_by` | `string` | ❌ | `-updated_at` | 排序字段及顺序：<br>• `created_at`：按创建时间升序<br>• `-created_at`：按创建时间倒序<br>• `updated_at`：按更新时间升序<br>• `-updated_at`：按更新时间倒序（默认） |

> 💡 注意：字段前加 `-` 表示**倒序（descending）**，不加表示**升序（ascending）**。

---

## 响应（Response）

- **Content-Type**: `application/json`

### 响应结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `limit` | `int` | 实际返回的条数（可能受系统限制） |
| `has_more` | `bool` | 是否还有更多数据可供分页 |
| `data` | `array[object]` | 会话列表 |

### `data` 数组项结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `string` | 会话唯一 ID |
| `name` | `string` | 会话名称（通常由大语言模型自动生成） |
| `inputs` | `object` | 用户在该会话中传入的初始变量（键值对） |
| `status` | `string` | 会话状态（如 `"normal"`） |
| `introduction` | `string` | 开场白（部分应用配置下存在） |
| `created_at` | `timestamp` (int) | 会话创建时间（Unix 秒时间戳） |
| `updated_at` | `timestamp` (int) | 会话最后更新时间（Unix 秒时间戳） |

---

## 响应示例

```json
{
  "limit": 20,
  "has_more": false,
  "data": [
    {
      "id": "10799fb8-64f7-4296-bbf7-b42bfbe0ae54",
      "name": "New chat",
      "inputs": {
        "book": "book",
        "myName": "Lucy"
      },
      "status": "normal",
      "introduction": "",
      "created_at": 1679667915,
      "updated_at": 1679667915
    },
    {
      "id": "hSIhXBhNe8X1d8Et",
      "name": "iPhone 13 Pro Max specs",
      "inputs": {},
      "status": "normal",
      "introduction": "",
      "created_at": 1679667800,
      "updated_at": 1679667850
    }
  ]
}
```

---

## 使用建议

- **分页**：首次请求可省略 `last_id`；后续请求将上一页最后一条的 `id` 作为 `last_id` 传入。
- **性能**：避免设置过大的 `limit`（如接近 100），以减少响应延迟。
- **排序**：若需按时间线展示历史对话，推荐使用默认的 `-updated_at`。

--- 

> 📌 注意：此接口仅返回**当前用户**（由 `user` 参数指定）的会话，不同用户的会话相互隔离。