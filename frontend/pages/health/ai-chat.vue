<template>
  <view class="page">
    <view class="topbar-fixed">
      <view class="topbar-actions-fixed">
        <view class="btn-selector" @click="toggleSelector">
          <text class="icon-selector">📋</text>
        </view>
        <view class="btn-new-chat-icon" @click="clearChat">
          <text class="icon-new">➕</text>
        </view>
      </view>
      <view v-if="conversationSelectorVisible" class="selector-fixed-wrapper">
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
                <view v-if="m.role === 'ai' && !m.content && !m.thinking && loading" class="typing-indicator">
                  <view class="typing-dot"></view>
                  <view class="typing-dot"></view>
                  <view class="typing-dot"></view>
                </view>
                <template v-else>
                  <!-- 思考内容 -->
                  <view v-if="m.role === 'ai' && m.thinking !== undefined && m.thinking !== null" class="thinking-content">
                    <view class="thinking-header" @click="toggleThinking(i)">
                      <view class="thinking-label">思考过程</view>
                      <text class="thinking-toggle">{{ m.thinkingExpanded ? '收起' : '展开' }}</text>
                    </view>
                    <view v-if="m.thinkingExpanded" class="thinking-text-wrapper">
                      <text class="thinking-text">{{ m.thinking || '' }}</text>
                    </view>
                  </view>
                  <!-- 正式回答 -->
                  <text v-if="m.content" class="bubble-text">{{ m.content }}</text>
                </template>
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
      conversationId: "",
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
      messagesLoading: false,
      _isPageDestroyed: false,
      _timers: []
    };
  },
  onLoad() {
    // 每次进入页面都创建新会话
    this.conversationId = "";
    uni.removeStorageSync("ai_conversation_id");
    this.messages = [
      {
        role: "ai",
        content: "你好，我是 AI 健康助手。你可以描述症状或问题，我会给出科普建议。",
        timestamp: Date.now()
      }
    ];
    this.fetchConversations();
  },
  onShow() {
    this.fetchConversations();
  },
  onUnload() {
    this._isPageDestroyed = true;
    this._timers.forEach(timerId => {
      if (timerId) {
        clearTimeout(timerId);
      }
    });
    this._timers = [];
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
            const parsed = this.parseStreamText(msg.answer);
            // 如果有正式回答内容，默认收起思考内容；否则默认展开
            const hasAnswer = parsed.answer && parsed.answer.trim();
            formattedMessages.push({
              role: "ai",
              content: parsed.answer,
              thinking: parsed.thinking,
              thinkingExpanded: !hasAnswer,
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
      if (this._scrollLock || this._isPageDestroyed) return;
      this._scrollLock = true;
      this.$nextTick(() => {
        if (this._isPageDestroyed) return;
        const lastIndex = this.messages.length - 1;
        if (lastIndex >= 0) {
          this.scrollIntoView = `msg-${lastIndex}`;
        }
        const timerId = setTimeout(() => {
          if (!this._isPageDestroyed) {
            this.scrollTop += 999999;
            this._scrollLock = false;
          }
        }, 100);
        this._timers.push(timerId);
      });
    },

    onInputFocus() {
      if (this._isPageDestroyed) return;
      this.inputFocused = true;
      const timerId = setTimeout(() => {
        if (!this._isPageDestroyed) {
          this.bumpScroll();
        }
      }, 300);
      this._timers.push(timerId);
    },

    onInputBlur() {
      this.inputFocused = false;
    },

    clearChat() {
      uni.showModal({
        title: "新会话",
        content: "确定要创建新会话吗？当前对话将被保存。",
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
      if (!data) {
        return { thinking: "", answer: "" };
      }
      
      // 如果 data 是字符串，可能是 SSE 格式的文本，需要解析
      if (typeof data === "string") {
        // 尝试解析 SSE 格式的文本
        const sseEvents = data.split('\n\n').filter(line => line.trim().startsWith('data:'));
        if (sseEvents.length > 0) {
          // 累积所有 message 事件中的 answer 片段
          let accumulatedAnswer = "";
          let finalAnswer = null;
          
          // 先查找 message_end 事件，它包含完整的答案
          for (let i = sseEvents.length - 1; i >= 0; i--) {
            const line = sseEvents[i];
            const jsonStr = line.replace(/^data:\s*/, '').trim();
            if (!jsonStr) continue;
            try {
              const event = JSON.parse(jsonStr);
              
              if (event.event === 'message_end') {
                // message_end 事件可能包含完整的答案
                if (event.answer) {
                  finalAnswer = event.answer;
                  break;
                } else if (event.outputs && event.outputs.answer) {
                  finalAnswer = event.outputs.answer;
                  break;
                } else if (event.data && event.data.outputs && event.data.outputs.answer) {
                  finalAnswer = event.data.outputs.answer;
                  break;
                }
              }
            } catch (e) {
              // 忽略解析错误
            }
          }
          
          // 如果没有找到 message_end，则累积所有 message 事件的 answer
          if (!finalAnswer) {
            for (let i = 0; i < sseEvents.length; i++) {
              const line = sseEvents[i];
              const jsonStr = line.replace(/^data:\s*/, '').trim();
              if (!jsonStr) continue;
              try {
                const event = JSON.parse(jsonStr);
                if (event.event === 'message' && event.answer) {
                  accumulatedAnswer += event.answer;
                } else if (event.event === 'agent_message' && (event.message || event.answer)) {
                  accumulatedAnswer += (event.message || event.answer);
                }
              } catch (e) {
                // 忽略解析错误
              }
            }
            
            if (accumulatedAnswer) {
              finalAnswer = accumulatedAnswer;
            }
          }
          
          // 如果还是没找到，尝试查找 workflow_finished 事件
          if (!finalAnswer) {
            for (let i = sseEvents.length - 1; i >= 0; i--) {
              const line = sseEvents[i];
              const jsonStr = line.replace(/^data:\s*/, '').trim();
              if (!jsonStr) continue;
              try {
                const event = JSON.parse(jsonStr);
                if (event.event === 'workflow_finished' && event.data && event.data.outputs && event.data.outputs.answer) {
                  finalAnswer = event.data.outputs.answer;
                  break;
                }
              } catch (e) {
                // 忽略解析错误
              }
            }
          }
          
          if (finalAnswer) {
            return this.parseStreamText(finalAnswer);
          }
        }
        // 如果不是 SSE 格式，直接返回字符串
        return this.parseStreamText(data);
      }
      
      let text = "";
      
      // 按优先级尝试不同的数据路径
      if (data.answer) {
        // 直接有 answer 字段
        text = data.answer;
      } else if (data.outputs && data.outputs.answer) {
        // outputs.answer（workflow_finished 事件格式）
        text = data.outputs.answer;
      } else if (data.data) {
        if (typeof data.data === "string") {
          text = data.data;
        } else if (data.data.answer) {
          // data.data.answer
          text = data.data.answer;
        } else if (data.data.outputs && data.data.outputs.answer) {
          // data.data.outputs.answer（嵌套结构）
          text = data.data.outputs.answer;
        }
      } else {
        text = data.msg || data.message || "";
      }
      
      // 如果 text 还是空的，尝试从整个对象中递归查找 answer 字段
      if (!text && typeof data === "object") {
        const findAnswer = (obj, depth = 0) => {
          if (depth > 5 || !obj || typeof obj !== "object") return null; // 限制递归深度
          if (obj.answer && typeof obj.answer === "string") return obj.answer;
          for (const key in obj) {
            if (key === "answer" && typeof obj[key] === "string") {
              return obj[key];
            }
            const result = findAnswer(obj[key], depth + 1);
            if (result) return result;
          }
          return null;
        };
        const found = findAnswer(data);
        if (found) {
          text = found;
        }
      }
      
      const result = this.parseStreamText(text);
      
      return result;
    },

    parseStreamText(text) {
      if (!text || typeof text !== "string") {
        return { thinking: "", answer: "" };
      }
      
      let thinking = "";
      let answer = text;
      
      // 检测是否有思考标签开始
      const thinkingStartIndex = text.search(/<think>/i);
      
      if (thinkingStartIndex !== -1) {
        // 查找结束标签位置
        const thinkingEndMatch = text.match(/<\/redacted_reasoning>/i) || text.match(/<\/think>/i);
        
        if (thinkingEndMatch) {
          // 有完整标签对：提取完整思考内容
          const thinkingMatch = text.match(/<think>([\s\S]*?)<\/redacted_reasoning>/i) || 
                               text.match(/<think>([\s\S]*?)<\/think>/i);
          thinking = thinkingMatch ? thinkingMatch[1].trim() : "";
          // 移除完整标签对
          answer = text
            .replace(/<think>[\s\S]*?<\/redacted_reasoning>/gi, "")
            .replace(/<think>[\s\S]*?<\/think>/gi, "")
            .replace(/\n+/g, "\n")
            .trim();
        } else {
          // 只有开始标签，没有结束标签：提取从开始标签到文本末尾的内容
          const thinkingStartTag = text.match(/<think>/i);
          if (thinkingStartTag) {
            const startPos = thinkingStartTag.index + thinkingStartTag[0].length;
            thinking = text.substring(startPos).trim();
            // 移除开始标签及之后的内容（因为都在思考中）
            answer = text.substring(0, thinkingStartTag.index)
              .replace(/\n+/g, "\n")
              .trim();
          }
        }
      }
      
      return { thinking, answer };
    },

    toggleThinking(index) {
      if (this.messages[index] && this.messages[index].role === "ai") {
        this.$set(this.messages[index], "thinkingExpanded", !this.messages[index].thinkingExpanded);
      }
    },

    typewriterEffect(aiIndex, fullText, isThinking = false) {
      if (!fullText || !fullText.trim() || this._isPageDestroyed) return Promise.resolve();
      
      return new Promise((resolve) => {
        const chars = fullText.split('');
        let currentIndex = 0;
        const delay = () => Math.random() * 15 + 20; // 20-35ms 随机延时
        
        const typeNextChar = () => {
          if (this._isPageDestroyed || currentIndex >= chars.length) {
            resolve();
            return;
          }
          
          // 添加下一个字符
          const currentText = chars.slice(0, currentIndex + 1).join('');
          
          if (isThinking) {
            this.messages[aiIndex].thinking = currentText;
          } else {
            this.messages[aiIndex].content = currentText;
          }
          
          this.$nextTick(() => this.bumpScroll());
          currentIndex++;
          
          const timerId = setTimeout(typeNextChar, delay());
          this._timers.push(timerId);
        };
        
        typeNextChar();
      });
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
      const aiMsg = { role: "ai", content: "", thinking: undefined, thinkingExpanded: true, timestamp: Date.now() };
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

        // 检测是否为真正的 H5 浏览器环境
        let isH5;
        // #ifdef H5
        isH5 = typeof window !== "undefined" && typeof fetch !== "undefined";
        // #endif
        // #ifndef H5
        isH5 = false; // 非 H5 环境（小程序、APP等）强制使用 blocking
        // #endif

        if (isH5) {
          let reply = "";
          let cid = "";
          let messageId = "";
          
          // 打字机效果：队列和延时处理
          const chunkQueue = [];
          let isProcessingQueue = false;
          let streamEnded = false;
          
          // 处理队列中的chunk，带随机延时
          const processChunkQueue = () => {
            if (this._isPageDestroyed || isProcessingQueue || chunkQueue.length === 0) return;
            
            isProcessingQueue = true;
            const chunk = chunkQueue.shift();
            
            if (chunk && !this._isPageDestroyed) {
              reply += chunk;
              const parsed = this.parseStreamText(reply);
              // parseStreamText 在检测到开始标签时会返回 thinking（即使为空字符串）
              // 如果检测到开始标签，确保设置 thinking 字段（用于显示思考组件）
              if (reply.includes('<think>')) {
                this.messages[aiIndex].thinking = parsed.thinking !== undefined ? parsed.thinking : '';
              } else if (parsed.thinking) {
                this.messages[aiIndex].thinking = parsed.thinking;
              }
              this.messages[aiIndex].content = parsed.answer;
              // 当出现正式回答内容时，自动收起思考内容
              if (parsed.answer && parsed.answer.trim()) {
                this.messages[aiIndex].thinkingExpanded = false;
              }
              this.$nextTick(() => this.bumpScroll());
            }
            
            // 随机延时
            const delay = Math.random() * 15 + 20;
            const timerId = setTimeout(() => {
              if (!this._isPageDestroyed) {
                isProcessingQueue = false;
                // 如果队列还有内容或流未结束，继续处理
                if (chunkQueue.length > 0 || !streamEnded) {
                  processChunkQueue();
                }
              }
            }, delay);
            this._timers.push(timerId);
          };

          await requestSse({
            url: API.aiChatMessages, // 必须是 /ai/chat-messages
            method: "POST",
            data: payload,
            onEvent: (evt) => {
              const type = evt?.event || "";
              
              // 更新 conversation_id
              if (evt?.conversation_id) {
                cid = evt.conversation_id;
              }
              
              // 更新 message_id（用于识别同一消息的多个片段）
              if (evt?.message_id || evt?.id) {
                messageId = evt.message_id || evt.id;
              }

              // 处理流式消息片段
              if (type === "message") {
                const chunk = evt?.answer || "";
                if (chunk) {
                  chunkQueue.push(chunk);
                  processChunkQueue();
                }
              }

              // 处理 agent_message 事件（兼容）
              if (type === "agent_message") {
                const chunk = evt?.message || evt?.answer || "";
                if (chunk) {
                  chunkQueue.push(chunk);
                  processChunkQueue();
                }
              }

              // 处理错误事件
              if (type === "error") {
                streamEnded = true;
                throw new Error(evt?.message || evt?.msg || "AI stream error");
              }

              // 消息结束时，做最终解析
              if (type === "message_end") {
                streamEnded = true;
                if (evt?.conversation_id) {
                  cid = evt.conversation_id;
                }
                // 等待队列处理完成后再做最终解析
                const waitQueueEmpty = () => {
                  if (this._isPageDestroyed) return;
                  if (chunkQueue.length > 0 || isProcessingQueue) {
                    const timerId = setTimeout(waitQueueEmpty, 100);
                    this._timers.push(timerId);
                  } else {
                    const parsed = this.parseStreamText(reply);
                    // 如果检测到开始标签，即使thinking为空字符串也要设置
                    if (reply.includes('<think>')) {
                      this.messages[aiIndex].thinking = parsed.thinking !== undefined ? parsed.thinking : '';
                    } else if (parsed.thinking) {
                      this.messages[aiIndex].thinking = parsed.thinking;
                    }
                    this.messages[aiIndex].content = parsed.answer || this.messages[aiIndex].content;
                    // 当出现正式回答内容时，自动收起思考内容
                    if (parsed.answer && parsed.answer.trim()) {
                      this.messages[aiIndex].thinkingExpanded = false;
                    }
                    this.$nextTick(() => this.bumpScroll());
                  }
                };
                waitQueueEmpty();
              }
            }
          });

          // 等待队列处理完成后再做最终解析（防止遗漏）
          const waitFinalProcess = () => {
            if (this._isPageDestroyed) return;
            if (chunkQueue.length > 0 || isProcessingQueue) {
              const timerId = setTimeout(waitFinalProcess, 100);
              this._timers.push(timerId);
            } else {
              if (reply && !this._isPageDestroyed) {
                const parsed = this.parseStreamText(reply);
                this.messages[aiIndex].thinking = parsed.thinking;
                this.messages[aiIndex].content = parsed.answer;
                // 当出现正式回答内容时，自动收起思考内容
                if (parsed.answer && parsed.answer.trim()) {
                  this.messages[aiIndex].thinkingExpanded = false;
                }
                this.$nextTick(() => this.bumpScroll());
              }
            }
          };
          waitFinalProcess();

          if (cid) {
            this.conversationId = cid;
            uni.setStorageSync("ai_conversation_id", cid);
            // 发送消息后刷新会话列表
            this.fetchConversations();
          }

          if (!this.messages[aiIndex].content || this.messages[aiIndex].content.trim() === "") {
            this.messages[aiIndex].content = "抱歉，没有收到回复，请稍后重试。";
          }
          // 更新消息时间戳
          this.messages[aiIndex].timestamp = Date.now();
        } else {
          // 非 H5：走 blocking（uni.request 没法读 SSE 流）
          try {
            const data = await request({
              url: API.aiChatMessages,
              method: "POST",
              data: { ...payload, responseMode: "blocking" }
            });

            const cid =
              data?.conversation_id ||
              data?.conversationId ||
              data?.conversationID ||
              data?.data?.conversation_id ||
              data?.data?.conversationId ||
              "";
            if (cid) {
              this.conversationId = cid;
              uni.setStorageSync("ai_conversation_id", cid);
              this.fetchConversations();
            }

            const parsed = this.normalizeBlockingReply(data);
            
            // 使用打字机效果显示
            try {
              if (parsed.thinking && parsed.thinking.trim()) {
                // 先显示思考内容
                this.messages[aiIndex].thinking = "";
                await this.typewriterEffect(aiIndex, parsed.thinking, true);
                
                // 思考内容显示完成后，如果有正式回答，自动收起思考
                if (parsed.answer && parsed.answer.trim()) {
                  this.messages[aiIndex].thinkingExpanded = false;
                }
              }

              if (parsed.answer && parsed.answer.trim()) {
                // 显示正式回答
                this.messages[aiIndex].content = "";
                await this.typewriterEffect(aiIndex, parsed.answer, false);
              } else {
                this.messages[aiIndex].content = "抱歉，没有收到回复，请稍后重试。";
              }
            } catch (typewriterError) {
              // 如果打字机效果失败，直接显示完整内容作为降级方案
              this.messages[aiIndex].thinking = parsed.thinking || "";
              this.messages[aiIndex].content = parsed.answer || "抱歉，没有收到回复，请稍后重试。";
              if (parsed.answer && parsed.answer.trim()) {
                this.messages[aiIndex].thinkingExpanded = false;
              }
            }
            
            this.messages[aiIndex].timestamp = Date.now();
          } catch (blockingError) {
            console.error("[blocking mode] 请求失败:", blockingError);
            this.messages[aiIndex].thinking = "";
            this.messages[aiIndex].content = "AI 服务暂时不可用，请稍后再试。";
            this.messages[aiIndex].timestamp = Date.now();
            uni.showToast({
              title: blockingError?.msg || blockingError?.message || "请求失败",
              icon: "none",
              duration: 2000
            });
          }
        }
      } catch (e) {
        this.messages[aiIndex].thinking = "";
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
  padding-top: calc(var(--status-bar-height) + 100rpx);
}

.topbar-fixed {
  position: fixed;
  top: calc(var(--status-bar-height));
  right: 24rpx;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.topbar-actions-fixed {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.btn-selector,
.btn-new-chat-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.btn-selector:active,
.btn-new-chat-icon:active {
  transform: scale(0.95);
  box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.15);
}

.icon-selector,
.icon-new {
  font-size: 32rpx;
}

.selector-fixed-wrapper {
  position: relative;
  width: 400rpx;
}

.chat {
  flex: 1;
  padding: 24rpx;
  overflow-y: auto;
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
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
  background: #4a5568;
  color: #fff;
}

.avatar-user {
  background: #718096;
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
  background: #4a5568;
  color: #ffffff;
  border-bottom-right-radius: 8rpx;
}

.bubble-text {
  font-size: 30rpx;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking-content {
  margin-bottom: 16rpx;
  background: #f7fafc;
  border-radius: 12rpx;
  border-left: 4rpx solid #cbd5e0;
  overflow: hidden;
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx;
  cursor: pointer;
  user-select: none;
}

.thinking-label {
  font-size: 24rpx;
  color: #718096;
  font-weight: 600;
}

.thinking-toggle {
  font-size: 22rpx;
  color: #4a5568;
  opacity: 0.7;
}

.thinking-text-wrapper {
  padding: 0 16rpx 16rpx 16rpx;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 2000rpx;
  }
}

.thinking-text {
  font-size: 26rpx;
  color: #4a5568;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  opacity: 0.8;
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
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e2e8f0;
  box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.08);
  z-index: 1001;
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
  border-color: #4a5568;
  box-shadow: 0 0 0 4rpx rgba(74, 85, 104, 0.1);
}

.composer-btn {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #4a5568;
  color: #fff;
  font-size: 36rpx;
  border: none;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
}

.composer-btn:not(.disabled):active {
  transform: scale(0.95);
  box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.2);
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
