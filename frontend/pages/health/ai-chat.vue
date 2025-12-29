<template>
  <view class="page">
    <view class="topbar">
      <view class="topbar-title">AI 健康助手</view>
      <view class="topbar-sub">随时问我健康相关问题（仅科普建议）</view>
      <view class="topbar-actions">
        <view class="btn-ghost" @click="clearChat">清空</view>
      </view>
    </view>

    <scroll-view
      class="chat"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
    >
      <view class="chat-inner">
        <view
          v-for="(m, i) in messages"
          :key="i"
          class="row"
          :class="m.role === 'user' ? 'row-user' : 'row-ai'"
        >
          <view v-if="m.role === 'ai'" class="avatar avatar-ai">AI</view>
          <view class="bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <text class="bubble-text">{{ m.content }}</text>
          </view>
          <view v-if="m.role === 'user'" class="avatar avatar-user">我</view>
        </view>
      </view>
    </scroll-view>

    <view class="composer">
      <input
        class="composer-input"
        v-model="input"
        :disabled="loading"
        placeholder="输入你的问题，比如：我头痛..."
        confirm-type="send"
        @confirm="send"
      />
      <button class="composer-btn" :disabled="loading || !input.trim()" @click="send">
        {{ loading ? "发送中" : "发送" }}
      </button>
    </view>
  </view>
</template>

<script>
import { request, requestSse, API } from "@/common/request";

export default {
  data() {
    return {
      input: "",
      loading: false,
      scrollTop: 0,
      conversationId: uni.getStorageSync("ai_conversation_id") || "",
      messages: [
        {
          role: "ai",
          content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。"
        }
      ],
      _scrollLock: false
    };
  },
  methods: {
    bumpScroll() {
      if (this._scrollLock) return;
      this._scrollLock = true;
      setTimeout(() => {
        this.scrollTop += 999999;
        this._scrollLock = false;
      }, 30);
    },

    clearChat() {
      this.messages = [
        {
          role: "ai",
          content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。"
        }
      ];
      this.input = "";
      this.loading = false;
      this.conversationId = "";
      uni.removeStorageSync("ai_conversation_id");
      this.$nextTick(() => this.bumpScroll());
    },

    normalizeBlockingReply(data) {
      if (!data) return "";
      if (typeof data === "string") return data;
      if (data.answer) return data.answer;
      if (data.data && data.data.answer) return data.data.answer;
      if (data.data && typeof data.data === "string") return data.data;
      return data.msg || data.message || "";
    },

    async send() {
      const content = (this.input || "").trim();
      if (!content || this.loading) return;

      this.loading = true;
      this.input = "";

      // 用户消息
      this.messages.push({ role: "user", content });
      this.$nextTick(() => this.bumpScroll());

      // AI 占位消息（流式更新）
      const aiMsg = { role: "ai", content: "" };
      this.messages.push(aiMsg);
      const aiIndex = this.messages.length - 1;
      this.$nextTick(() => this.bumpScroll());

      try {
        const payload = {
          query: content,
          responseMode: "streaming",
          conversationId: this.conversationId || "",
          inputs: {},
          autoGenerateName: true
        };

        const isH5 = typeof window !== "undefined" && typeof fetch !== "undefined";

        if (isH5) {
          let reply = "";
          let cid = "";

          await requestSse({
            url: API.aiChatMessages, // ✅ 必须是 /ai/chat-messages
            method: "POST",
            data: payload,
            onEvent: (evt) => {
              const type = evt?.event || "";
              if (evt?.conversation_id) cid = evt.conversation_id;

              if (type === "message" || type === "agent_message") {
                const chunk = evt?.answer || evt?.message || "";
                if (chunk) {
                  reply += chunk;
                  this.messages[aiIndex].content = reply;
                  this.$nextTick(() => this.bumpScroll());
                }
              }

              if (type === "error") {
                throw new Error(evt?.message || "AI stream error");
              }

              if (type === "message_end") {
                if (evt?.conversation_id) cid = evt.conversation_id;
              }
            }
          });

          if (cid) {
            this.conversationId = cid;
            uni.setStorageSync("ai_conversation_id", cid);
          }

          if (!this.messages[aiIndex].content) {
            this.messages[aiIndex].content = "（没有拿到回复内容）";
          }
        } else {
          // 非 H5：走 blocking（uni.request 没法读 SSE 流）
          const data = await request({
            url: API.aiChatMessages,
            method: "POST",
            data: { ...payload, responseMode: "blocking" }
          });

          const cid =
            data?.conversation_id ||
            data?.conversationId ||
            data?.conversationID ||
            "";
          if (cid) {
            this.conversationId = cid;
            uni.setStorageSync("ai_conversation_id", cid);
          }

          this.messages[aiIndex].content =
            this.normalizeBlockingReply(data) || "（没有拿到回复内容）";
        }
      } catch (e) {
        this.messages[aiIndex].content = "AI 服务暂时不可用，请稍后再试。";
      } finally {
        this.loading = false;
        this.$nextTick(() => this.bumpScroll());
      }
    }
  }
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f7fb;
  display: flex;
  flex-direction: column;
}

.topbar {
  padding: 22rpx 24rpx 18rpx;
  background: linear-gradient(180deg, #0b1220 0%, #111827 100%);
  color: #fff;
  position: relative;
}

.topbar-title {
  text-align: center;
  font-size: 34rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
}

.topbar-sub {
  text-align: center;
  margin-top: 8rpx;
  font-size: 24rpx;
  opacity: 0.7;
}

.topbar-actions {
  position: absolute;
  right: 18rpx;
  top: 18rpx;
}

.btn-ghost {
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.chat {
  flex: 1;
  padding: 24rpx;
}

.chat-inner {
  padding-bottom: 80rpx;
}

.row {
  display: flex;
  align-items: flex-end;
  margin-bottom: 18rpx;
}

.row-ai {
  justify-content: flex-start;
}

.row-user {
  justify-content: flex-end;
}

.avatar {
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 800;
}

.avatar-ai {
  background: #eef1ff;
  color: #3a5bfd;
  margin-right: 14rpx;
}

.avatar-user {
  background: #111827;
  color: #fff;
  margin-left: 14rpx;
}

.bubble {
  max-width: 72%;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  line-height: 1.6;
  box-shadow: 0 6rpx 18rpx rgba(17, 24, 39, 0.06);
}

.bubble-ai {
  background: #ffffff;
  color: #111827;
  border: 1rpx solid #eef0f4;
}

.bubble-user {
  background: linear-gradient(135deg, #111827 0%, #2f3542 100%);
  color: #ffffff;
}

.bubble-text {
  font-size: 28rpx;
  white-space: pre-wrap;
  word-break: break-word;
}

.composer {
  background: #ffffff;
  padding: 18rpx 18rpx env(safe-area-inset-bottom);
  display: flex;
  align-items: center;
  gap: 14rpx;
  border-top: 1rpx solid #eef0f4;
}

.composer-input {
  flex: 1;
  height: 74rpx;
  border-radius: 18rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  background: #f3f5f9;
}

.composer-btn {
  width: 160rpx;
  height: 74rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  color: #fff;
  background: #111827;
}

.composer-btn[disabled] {
  opacity: 0.5;
}
</style>
