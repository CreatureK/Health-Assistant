<template>
  <view class="page">
    <view class="topbar">
      <view class="topbar-header">
        <view class="topbar-title">AI 健康助手</view>
        <view class="topbar-actions">
          <view class="btn-icon" @click="clearChat">
            <text class="icon">🗑️</text>
          </view>
        </view>
      </view>
      <view class="topbar-sub">随时问我健康相关问题（仅科普建议）</view>
      <view class="topbar-selector-wrapper">
        <ConversationSelector
          :conversations="conversations"
          :current-id="conversationId"
          :loading="conversationsLoading"
          :visible="conversationSelectorVisible"
          @select="onSelectConversation"
          @toggle="toggleSelector"
        />
      </view>
    </view>

    <scroll-view
      class="chat"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-with-animation="true"
      :scroll-into-view="scrollIntoView"
    >
      <view class="chat-inner">
        <view v-if="messages.length === 1 && messages[0].role === 'ai'" class="welcome">
          <view class="welcome-icon">💬</view>
          <view class="welcome-title">开始对话</view>
          <view class="welcome-desc">描述你的健康问题，我会给出科普建议</view>
        </view>
        <view
          v-for="(m, i) in messages"
          :key="i"
          :id="`msg-${i}`"
          class="message-wrapper"
          :class="m.role === 'user' ? 'message-user' : 'message-ai'"
        >
          <view class="message-row">
            <view v-if="m.role === 'ai'" class="avatar avatar-ai">AI</view>
            <view class="message-content">
              <view class="bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
                <view v-if="m.role === 'ai' && !m.content && loading" class="typing-indicator">
                  <view class="typing-dot"></view>
                  <view class="typing-dot"></view>
                  <view class="typing-dot"></view>
                </view>
                <text v-else class="bubble-text">{{ m.content || "正在思考..." }}</text>
              </view>
              <view v-if="m.timestamp" class="message-time">{{ formatMessageTime(m.timestamp) }}</view>
            </view>
            <view v-if="m.role === 'user'" class="avatar avatar-user">我</view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="composer">
      <view class="composer-inner">
        <input
          class="composer-input"
          v-model="input"
          :disabled="loading"
          placeholder="输入你的问题..."
          confirm-type="send"
          @confirm="send"
          @focus="onInputFocus"
          @blur="onInputBlur"
        />
        <button
          class="composer-btn"
          :class="{ disabled: loading || !input.trim() }"
          :disabled="loading || !input.trim()"
          @click="send"
        >
          <text v-if="loading" class="btn-loading">⏳</text>
          <text v-else class="btn-icon">📤</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { request, requestSse, API } from "@/common/request";
import ConversationSelector from "./components/ConversationSelector.vue";

export default {
  components: {
    ConversationSelector
  },
  data() {
    return {
      input: "",
      loading: false,
      scrollTop: 0,
      scrollIntoView: "",
      conversationId: uni.getStorageSync("ai_conversation_id") || "",
      messages: [
        {
          role: "ai",
          content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。",
          timestamp: Date.now()
        }
      ],
      _scrollLock: false,
      conversations: [],
      conversationsLoading: false,
      conversationSelectorVisible: false,
      inputFocused: false,
      messagesLoading: false
    };
  },
  onLoad() {
    this.fetchConversations();
    if (this.conversationId) {
      this.fetchMessages(this.conversationId);
    }
  },
  onShow() {
    this.fetchConversations();
  },
  methods: {
    async fetchConversations() {
      this.conversationsLoading = true;
      try {
        const data = await request({
          url: API.aiConversations,
          method: "GET",
          data: {
            limit: 50
          }
        });
        // 响应结构：data.data 是会话列表数组
        this.conversations = Array.isArray(data?.data) ? data.data : [];
      } catch (e) {
        console.error("[fetchConversations failed]", e);
        this.conversations = [];
      } finally {
        this.conversationsLoading = false;
      }
    },

    toggleSelector() {
      this.conversationSelectorVisible = !this.conversationSelectorVisible;
    },

    async fetchMessages(conversationId) {
      if (!conversationId) return;
      
      this.messagesLoading = true;
      try {
        const data = await request({
          url: API.aiMessages,
          method: "GET",
          data: {
            conversation_id: conversationId,
            limit: 50
          }
        });
        
        // 响应结构：data.data 是消息列表数组
        const messageList = Array.isArray(data?.data) ? data.data : [];
        
        // 转换为前端消息格式
        const formattedMessages = [];
        for (const msg of messageList) {
          // 用户消息
          if (msg.query) {
            formattedMessages.push({
              role: "user",
              content: msg.query,
              timestamp: msg.createdAt ? msg.createdAt * 1000 : Date.now()
            });
          }
          
          // AI 消息
          if (msg.answer) {
            // 清理 <think> 标签（AI 内部推理过程）
            let cleanAnswer = msg.answer;
            cleanAnswer = cleanAnswer.replace(/<think>[\s\S]*?<\/redacted_reasoning>/g, "").trim();
            
            formattedMessages.push({
              role: "ai",
              content: cleanAnswer,
              timestamp: msg.createdAt ? msg.createdAt * 1000 : Date.now()
            });
          }
        }
        
        // 如果没有历史消息，显示欢迎语
        if (formattedMessages.length === 0) {
          this.messages = [
            {
              role: "ai",
              content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。",
              timestamp: Date.now()
            }
          ];
        } else {
          this.messages = formattedMessages;
        }
        
        this.$nextTick(() => this.bumpScroll());
      } catch (e) {
        console.error("[fetchMessages failed]", e);
        uni.showToast({
          title: e?.msg || e?.message || "加载消息失败",
          icon: "none",
          duration: 2000
        });
        // 失败时显示欢迎语
        this.messages = [
          {
            role: "ai",
            content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。",
            timestamp: Date.now()
          }
        ];
      } finally {
        this.messagesLoading = false;
      }
    },

    async onSelectConversation(conversation) {
      if (!conversation || !conversation.id) return;
      
      // 切换会话：更新 conversationId
      this.conversationId = conversation.id;
      uni.setStorageSync("ai_conversation_id", conversation.id);
      
      // 加载该会话的历史消息
      await this.fetchMessages(conversation.id);
    },

    formatMessageTime(timestamp) {
      if (!timestamp) return "";
      const date = new Date(timestamp);
      const now = new Date();
      const diff = now - date;
      const minutes = Math.floor(diff / 60000);
      
      if (minutes < 1) return "刚刚";
      if (minutes < 60) return `${minutes}分钟前`;
      
      const hours = Math.floor(minutes / 60);
      if (hours < 24) return `${hours}小时前`;
      
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hoursStr = String(date.getHours()).padStart(2, "0");
      const minutesStr = String(date.getMinutes()).padStart(2, "0");
      return `${month}-${day} ${hoursStr}:${minutesStr}`;
    },

    bumpScroll() {
      if (this._scrollLock) return;
      this._scrollLock = true;
      this.$nextTick(() => {
        const lastIndex = this.messages.length - 1;
        if (lastIndex >= 0) {
          this.scrollIntoView = `msg-${lastIndex}`;
        }
        setTimeout(() => {
          this.scrollTop += 999999;
          this._scrollLock = false;
        }, 100);
      });
    },

    onInputFocus() {
      this.inputFocused = true;
      setTimeout(() => this.bumpScroll(), 300);
    },

    onInputBlur() {
      this.inputFocused = false;
    },

    clearChat() {
      uni.showModal({
        title: "确认清空",
        content: "确定要清空当前对话吗？",
        success: (res) => {
          if (res.confirm) {
            this.messages = [
              {
                role: "ai",
                content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。",
                timestamp: Date.now()
              }
            ];
            this.input = "";
            this.loading = false;
            this.conversationId = "";
            uni.removeStorageSync("ai_conversation_id");
            this.fetchConversations();
            this.$nextTick(() => this.bumpScroll());
          }
        }
      });
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
      this.messages.push({ role: "user", content, timestamp: Date.now() });
      this.$nextTick(() => this.bumpScroll());

      // AI 占位消息（流式更新）
      const aiMsg = { role: "ai", content: "", timestamp: Date.now() };
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
            // 发送消息后刷新会话列表
            this.fetchConversations();
          }

          if (!this.messages[aiIndex].content) {
            this.messages[aiIndex].content = "抱歉，没有收到回复，请稍后重试。";
          }
          // 更新消息时间戳
          this.messages[aiIndex].timestamp = Date.now();
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
            this.fetchConversations();
          }

          this.messages[aiIndex].content =
            this.normalizeBlockingReply(data) || "抱歉，没有收到回复，请稍后重试。";
          this.messages[aiIndex].timestamp = Date.now();
        }
      } catch (e) {
        this.messages[aiIndex].content = "AI 服务暂时不可用，请稍后再试。";
        this.messages[aiIndex].timestamp = Date.now();
        uni.showToast({
          title: e?.msg || e?.message || "请求失败",
          icon: "none",
          duration: 2000
        });
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
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.topbar {
  padding: calc(var(--status-bar-height) + 16rpx) 24rpx 20rpx;
  background: linear-gradient(180deg, #1a1f2e 0%, #2d3748 100%);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.topbar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.topbar-title {
  font-size: 36rpx;
  font-weight: 700;
  letter-spacing: 0.5rpx;
}

.topbar-sub {
  font-size: 24rpx;
  opacity: 0.85;
  margin-bottom: 16rpx;
}

.topbar-selector-wrapper {
  margin-top: 8rpx;
}

.topbar-actions {
  display: flex;
  align-items: center;
}

.btn-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  transition: all 0.2s;
}

.btn-icon:active {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(0.95);
}

.btn-icon .icon {
  font-size: 32rpx;
}

.chat {
  flex: 1;
  padding: 24rpx;
  overflow-y: auto;
}

.chat-inner {
  padding-bottom: 40rpx;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  text-align: center;
}

.welcome-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
  opacity: 0.8;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 16rpx;
}

.welcome-desc {
  font-size: 28rpx;
  color: #718096;
  line-height: 1.6;
}

.message-wrapper {
  margin-bottom: 32rpx;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.message-user {
  flex-direction: row-reverse;
}

.message-content {
  flex: 1;
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.message-user .message-content {
  align-items: flex-end;
}

.message-ai .message-content {
  align-items: flex-start;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.avatar-ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.avatar-user {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.bubble {
  padding: 20rpx 24rpx;
  border-radius: 24rpx;
  line-height: 1.7;
  word-wrap: break-word;
  position: relative;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.bubble-ai {
  background: #ffffff;
  color: #2d3748;
  border: 1rpx solid #e2e8f0;
  border-bottom-left-radius: 8rpx;
}

.bubble-user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-bottom-right-radius: 8rpx;
}

.bubble-text {
  font-size: 30rpx;
  white-space: pre-wrap;
  word-break: break-word;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 0;
}

.typing-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #a0aec0;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10rpx);
    opacity: 1;
  }
}

.message-time {
  font-size: 22rpx;
  color: #a0aec0;
  margin-top: 8rpx;
  padding: 0 4rpx;
}

.composer {
  background: #ffffff;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e2e8f0;
  box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.04);
}

.composer-inner {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.composer-input {
  flex: 1;
  height: 80rpx;
  border-radius: 40rpx;
  padding: 0 28rpx;
  font-size: 30rpx;
  background: #f7fafc;
  border: 2rpx solid transparent;
  transition: all 0.2s;
}

.composer-input:focus {
  background: #ffffff;
  border-color: #667eea;
  box-shadow: 0 0 0 4rpx rgba(102, 126, 234, 0.1);
}

.composer-btn {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 36rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.3);
  transition: all 0.2s;
}

.composer-btn:not(.disabled):active {
  transform: scale(0.95);
  box-shadow: 0 2rpx 8rpx rgba(102, 126, 234, 0.4);
}

.composer-btn.disabled {
  opacity: 0.4;
  background: #cbd5e0;
  box-shadow: none;
}

.btn-loading,
.btn-icon {
  font-size: 36rpx;
}
</style>
