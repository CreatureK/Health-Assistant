// common/request.js
import { API } from "./api";

// BASE_URL：优先环境变量，其次回退本地后端
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
        // HTTP 层成功
        if (!(res.statusCode >= 200 && res.statusCode < 300)) {
          reject(res);
          return;
        }
      
        const r = res.data;
      
        // 按接口文档：统一返回 { code, msg, data }
        if (r && typeof r === "object" && Object.prototype.hasOwnProperty.call(r, "code")) {
          if (r.code === 200) {
            // 只把 payload(data) 返回给页面，页面就能直接 data.token / data.list ...
            resolve(r.data);
          } else {
            // 业务失败：把 msg 透出，方便页面 toast
            reject(r);
          }
          return;
        }
      
        // 兜底：如果后端不是统一包装，就原样返回
        resolve(r);
      },

      fail(err) {
        reject(err);
      }
    });
  });
}

/**
 * SSE 流式请求（H5 专用：fetch + ReadableStream）
 * 后端返回 Content-Type: text/event-stream
 *
 * @param {Object} options
 * @param {string} options.url  eg：/ai/chat-messages
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
