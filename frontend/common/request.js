// common/request.js
import { API } from "./api";

// ✅ BASE_URL：优先环境变量，其次回退本地后端
// - vue-cli/uniapp：VUE_APP_BASE_URL
// - Vite：VITE_BASE_URL
const BASE_URL =
  (typeof process !== "undefined" &&
    process.env &&
    (process.env.VUE_APP_BASE_URL || process.env.VITE_BASE_URL)) ||
  "http://localhost:8080/api/v1";

function getToken() {
  return (
    uni.getStorageSync("token") ||
    uni.getStorageSync("Authorization") ||
    uni.getStorageSync("access_token") ||
    ""
  );
}

/**
 * 普通 HTTP 请求（uni.request）
 */
export function request({ url, method = "GET", data, header }) {
  return new Promise((resolve, reject) => {
    const token = getToken();

    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        "content-type": "application/json",
        ...(header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success(res) {
        // 兼容：你的后端可能直接返回 Result，也可能直接返回数据
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else {
          reject(res);
        }
      },
      fail(err) {
        reject(err);
      }
    });
  });
}

/**
 * ✅ SSE 流式请求（H5 专用：fetch + ReadableStream）
 * 后端返回 Content-Type: text/event-stream
 *
 * @param {Object} options
 * @param {string} options.url  例如：/ai/chat-messages
 * @param {string} [options.method="POST"]
 * @param {Object} options.data
 * @param {(evt:any)=>void} options.onEvent
 */
export async function requestSse({ url, method = "POST", data, onEvent }) {
  if (typeof window === "undefined" || typeof fetch === "undefined") {
    throw new Error("SSE only supported in H5 (browser) environment");
  }

  const token = getToken();

  const resp = await fetch(BASE_URL + url, {
    method,
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  });

  if (!resp.ok) {
    const text = await resp.text().catch(() => "");
    throw new Error(text || `HTTP ${resp.status}`);
  }

  if (!resp.body) {
    throw new Error("No response body (ReadableStream not supported)");
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE：事件之间用空行分隔
    const events = buffer.split("\n\n");
    buffer = events.pop() || "";

    for (const evt of events) {
      const lines = evt.split("\n");
      for (const line of lines) {
        if (!line.startsWith("data:")) continue;

        const payload = line.replace(/^data:\s*/, "").trim();
        if (!payload) continue;

        try {
          onEvent && onEvent(JSON.parse(payload));
        } catch {
          // 非 JSON 就忽略
        }
      }
    }
  }
}

export { API };
