<template>
  <view class="page">
    <!-- 顶部：会话栏 -->
    <view class="topbar">
      <view class="left">
        <picker
          :range="sessionTitles"
          :value="sessionIndex"
          @change="onPickSession"
        >
          <view class="picker">
            <text class="picker-label">会话：</text>
            <text class="picker-value">{{
              sessionTitles[sessionIndex] || "新会话"
            }}</text>
          </view>
        </picker>
      </view>

      <view class="right">
        <button class="btn ghost" size="mini" @click="newSession"
          >新建</button
        >
        <button class="btn ghost" size="mini" @click="refreshSessions"
          >刷新</button
        >
      </view>
    </view>

    <!-- 中间：消息区 -->
    <scroll-view
      class="chat"
      scroll-y
      :scroll-into-view="scrollIntoId"
      :scroll-with-animation="true"
    >
      <view v-for="m in messages" :key="m.id" :id="m.id" class="row">
        <view :class="['bubble', m.role === 'user' ? 'user' : 'assistant']">
          <text class="bubble-text">{{ m.content }}</text>

          <view v-if="m.role === 'assistant' && m.safetyHint" class="hint">
            {{ m.safetyHint }}
          </view>
        </view>
      </view>

      <view class="status" v-if="loading">
        <text>AI 正在回复…</text>
      </view>
    </scroll-view>

    <!-- 底部：输入区 -->
    <view class="composer">
      <input
        class="input"
        v-model="input"
        placeholder="请输入你的健康问题…"
        confirm-type="send"
        @confirm="send"
      />
      <button class="btn" :disabled="!input || loading" @click="send">
        发送
      </button>
    </view>
  </view>
</template>

<script>
import { request } from "@/common/request.js";
import { API } from "@/common/api.js";

function uid(prefix = "m") {
  return `${prefix}_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function buildQuery(params) {
  const clean = Object.entries(params || {}).filter(
    ([, v]) => v !== undefined && v !== null && v !== ""
  );
  if (clean.length === 0) return "";
  return (
    "?" +
    clean
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join("&")
  );
}

/**
 * 尝试解析 SSE/Chunk：
 * - 兼容 data: <text>
 * - 兼容 data: {"delta":"..."} / {"content":"..."} / {"replyText":"..."}
 * - 兼容直接返回纯文本 chunk
 */
function parseAndAppendSSEChunk(state, chunkText, onToken) {
  state.buffer += chunkText;

  // SSE 一般按 \n\n 分隔事件，这里尽量宽松处理
  const parts = state.buffer.split("\n");
  state.buffer = parts.pop() || "";

  for (const line of parts) {
    const s = String(line || "").trim();
    if (!s) continue;

    // SSE data:
    if (s.startsWith("data:")) {
      const payload = s.slice(5).trim();
      if (!payload) continue;
      if (payload === "[DONE]") {
        state.done = true;
        continue;
      }

      // JSON 或纯文本
      try {
        const obj = JSON.parse(payload);
        const token =
          obj.delta ||
          obj.content ||
          obj.reply ||
          obj.replyText ||
          obj.text ||
          obj.message ||
          "";
        if (token) onToken(token, obj);
      } catch (e) {
        onToken(payload, null);
      }
      continue;
    }

    // 非标准：直接是内容
    onToken(s, null);
  }
}

export default {
  data() {
    return {
      sessions: [],
      sessionIndex: 0,
      currentSessionId: "",
      messages: [],
      input: "",
      loading: false,
      scrollIntoId: "",
      // 如果后端在 assistant 回复里返回 safetyHint，这里会展示
      lastSafetyHint: ""
    };
  },

  computed: {
    sessionTitles() {
      // 这里不强依赖后端字段名：兼容 title/name/createdAt
      return this.sessions.map((s) => {
        const t = s.title || s.name;
        if (t) return t;
        if (s.createdAt) return `会话 ${String(s.createdAt).slice(0, 16)}`;
        return `会话 ${s.id || ""}`.trim();
      });
    }
  },

  onLoad() {
    this.refreshSessions();
  },

  methods: {
    scrollToBottom() {
      const last = this.messages[this.messages.length - 1];
      if (last) this.scrollIntoId = last.id;
    },

    async refreshSessions() {
      try {
        const list = (await request({ url: API.aiSessions })) || [];
        this.sessions = Array.isArray(list) ? list : [];

        // 选中：若当前 session 还在，则保持；否则选第一个；若没有则新会话
        const idx = this.sessions.findIndex(
          (s) => String(s.id) === String(this.currentSessionId)
        );
        if (idx >= 0) {
          this.sessionIndex = idx;
        } else {
          this.sessionIndex = 0;
          this.currentSessionId = this.sessions[0]?.id ? String(this.sessions[0].id) : "";
        }

        if (this.currentSessionId) {
          await this.loadHistory(this.currentSessionId);
        } else {
          this.messages = [];
        }
      } catch (e) {
        // request.js 已 toast，这里不重复
      }
    },

    async onPickSession(e) {
      const idx = Number(e?.detail?.value || 0);
      this.sessionIndex = idx;
      const s = this.sessions[idx];
      this.currentSessionId = s?.id ? String(s.id) : "";
      await this.loadHistory(this.currentSessionId);
    },

    async newSession() {
      this.currentSessionId = "";
      this.sessionIndex = 0;
      this.messages = [];
      this.lastSafetyHint = "";
      uni.showToast({ title: "已切到新会话", icon: "none" });
    },

    async loadHistory(sessionId) {
      if (!sessionId) {
        this.messages = [];
        return;
      }
      try {
        const list =
          (await request({ url: API.aiSessionMessages(sessionId) })) || [];

        // 兼容后端返回：
        // - 直接数组 [{role, content}]
        // - 或 {list:[...]}
        const arr = Array.isArray(list) ? list : list.list || [];
        this.messages = arr.map((x) => ({
          id: uid("h"),
          role: x.role || x.type || "assistant",
          content: x.content || x.text || x.message || "",
          safetyHint: x.safetyHint || ""
        }));

        this.$nextTick(() => this.scrollToBottom());
      } catch (e) {}
    },

    async send() {
      const text = (this.input || "").trim();
      if (!text || this.loading) return;

      this.input = "";

      // 1) 推入用户消息
      this.messages.push({ id: uid("u"), role: "user", content: text });
      // 2) 推入 assistant 占位（用于流式拼接）
      const assistantId = uid("a");
      this.messages.push({
        id: assistantId,
        role: "assistant",
        content: "",
        safetyHint: ""
      });

      this.$nextTick(() => this.scrollToBottom());

      // 3) 请求
      this.loading = true;
      this.lastSafetyHint = "";

      // 优先走“真流式”（小程序/APP），否则降级为普通 JSON
      const streamed = await this.tryStreamChat({
        sessionId: this.currentSessionId || undefined,
        message: text,
        inputType: "text"
      }, assistantId);

      if (!streamed) {
        await this.fallbackJsonChat(
          {
            sessionId: this.currentSessionId || undefined,
            message: text,
            inputType: "text"
          },
          assistantId
        );
      }

      this.loading = false;
      this.$nextTick(() => this.scrollToBottom());

      // 刷新会话列表（比如新会话创建后需要出现）
      this.refreshSessions();
    },

    async fallbackJsonChat(body, assistantId) {
      try {
        // 文档约定：POST /api/v1/ai/chat :contentReference[oaicite:8]{index=8}
        const res = await request({
          url: API.aiChat,
          method: "POST",
          data: body
        });

        // 兼容字段：replyText / text
        const reply = res?.replyText || res?.text || res?.reply || "";
        const safetyHint = res?.safetyHint || "";

        // 更新 sessionId
        if (res?.sessionId) this.currentSessionId = String(res.sessionId);

        const idx = this.messages.findIndex((m) => m.id === assistantId);
        if (idx >= 0) {
          this.messages[idx].content = reply || "（无内容）";
          this.messages[idx].safetyHint = safetyHint || "";
        }
      } catch (e) {
        const idx = this.messages.findIndex((m) => m.id === assistantId);
        if (idx >= 0) this.messages[idx].content = "请求失败，请稍后重试。";
      }
    },

    async tryStreamChat(body, assistantId) {
      // 只有 APP / 微信小程序端支持 onChunkReceived；H5 直接返回 false
      // （你后端说“流式传输”已实现，这里尽量用真实 chunk 来拼）
      let support = false;

      // #ifdef MP-WEIXIN
      support = true;
      // #endif
      // #ifdef APP-PLUS
      support = true;
      // #endif

      if (!support) return false;

      const token = uni.getStorageSync("token") || "";
      const state = { buffer: "", done: false };

      const appendToken = (tokenText, obj) => {
        const idx = this.messages.findIndex((m) => m.id === assistantId);
        if (idx < 0) return;

        this.messages[idx].content += tokenText;

        // 从任意 chunk 里捕获 sessionId / safetyHint
        if (obj && obj.sessionId) this.currentSessionId = String(obj.sessionId);
        if (obj && obj.safetyHint) this.messages[idx].safetyHint = obj.safetyHint;

        this.$nextTick(() => this.scrollToBottom());
      };

      return new Promise((resolve) => {
        const task = uni.request({
          url: BASE_URL + API.aiChat + buildQuery({ stream: 1 }), // 兼容：很多后端用 ?stream=1 开启流式
          method: "POST",
          data: body,
          header: {
            "content-type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            Accept: "text/event-stream"
          },
          // #ifdef MP-WEIXIN
          enableChunkedTransfer: true,
          // #endif
          // #ifdef APP-PLUS
          enableChunked: true,
          // #endif
          success: (res) => {
            // 如果后端其实没按流式返回，会直接一次性到这里
            // 尝试当文本解析一下，若解析不到，就返回 false 走 fallback
            const txt =
              typeof res.data === "string" ? res.data : JSON.stringify(res.data);
            if (!txt) return resolve(false);

            let anyToken = false;
            parseAndAppendSSEChunk(state, txt, (t) => {
              anyToken = true;
              appendToken(t, null);
            });

            resolve(anyToken);
          },
          fail: () => resolve(false)
        });

        // 真实 chunk 回调
        if (task && typeof task.onChunkReceived === "function") {
          task.onChunkReceived((res) => {
            try {
              // 微信/APP 返回可能是 ArrayBuffer
              let txt = "";
              if (res && res.data) {
                if (typeof res.data === "string") {
                  txt = res.data;
                } else {
                  // ArrayBuffer -> string
                  const u8 = new Uint8Array(res.data);
                  txt = String.fromCharCode.apply(null, Array.from(u8));
                }
              }
              if (!txt) return;

              parseAndAppendSSEChunk(state, txt, (tokenText, obj) => {
                appendToken(tokenText, obj);
              });

              if (state.done) resolve(true);
            } catch (e) {
              // ignore
            }
          });
        }
      });
    }
  }
};
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f7f7f7;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 20rpx;
  background: #ffffff;
  border-bottom: 1px solid #eee;
}

.picker {
  display: flex;
  align-items: center;
  padding: 8rpx 12rpx;
  border: 1px solid #eee;
  border-radius: 12rpx;
  background: #fafafa;
}
.picker-label {
  color: #666;
  font-size: 26rpx;
}
.picker-value {
  margin-left: 8rpx;
  font-size: 26rpx;
}

.btn {
  background: #2d6cdf;
  color: #fff;
  border-radius: 12rpx;
  padding: 0 18rpx;
  height: 64rpx;
  line-height: 64rpx;
}
.btn[disabled] {
  opacity: 0.5;
}
.btn.ghost {
  background: #fff;
  color: #2d6cdf;
  border: 1px solid #cfe0ff;
  margin-left: 10rpx;
}

.chat {
  flex: 1;
  padding: 18rpx 18rpx 0 18rpx;
}

.row {
  margin-bottom: 18rpx;
  display: flex;
}

.bubble {
  max-width: 78%;
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  line-height: 1.5;
  font-size: 28rpx;
  background: #fff;
  color: #222;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}

.bubble.user {
  margin-left: auto;
  background: #2d6cdf;
  color: #fff;
}

.bubble-text {
  white-space: pre-wrap;
}

.hint {
  margin-top: 10rpx;
  font-size: 24rpx;
  opacity: 0.85;
}

.status {
  text-align: center;
  color: #999;
  padding: 14rpx 0 24rpx;
}

.composer {
  display: flex;
  padding: 16rpx 16rpx;
  background: #ffffff;
  border-top: 1px solid #eee;
}

.input {
  flex: 1;
  background: #f7f7f7;
  border-radius: 12rpx;
  padding: 0 16rpx;
  height: 72rpx;
  line-height: 72rpx;
  margin-right: 12rpx;
}
</style>
