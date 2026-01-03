<template>
  <view class="selector-wrapper">
    <view class="selector-trigger" @click="handleToggle">
      <text class="trigger-text">{{ currentConversationName || "选择会话" }}</text>
      <text class="trigger-arrow" :class="{ open: visible }">▼</text>
    </view>

    <view v-if="visible" class="selector-dropdown">
      <view v-if="loading" class="dropdown-loading">加载中...</view>
      <view v-else-if="!conversations || conversations.length === 0" class="dropdown-empty">
        暂无会话
      </view>
      <scroll-view v-else class="dropdown-list" scroll-y>
        <view
          v-for="item in conversations"
          :key="item.id"
          class="dropdown-item"
          :class="{ active: item.id === currentId }"
          @click="handleSelect(item)"
        >
          <view class="item-name">{{ item.name || "未命名会话" }}</view>
          <view class="item-time">{{ formatTime(item.updatedAt) }}</view>
        </view>
      </scroll-view>
    </view>

    <view v-if="visible" class="selector-mask" @click="handleToggle"></view>
  </view>
</template>

<script>
export default {
  name: "ConversationSelector",
  props: {
    conversations: {
      type: Array,
      default: () => []
    },
    currentId: {
      type: String,
      default: ""
    },
    loading: {
      type: Boolean,
      default: false
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    currentConversationName() {
      if (!this.currentId || !this.conversations || this.conversations.length === 0) {
        return "";
      }
      const current = this.conversations.find((item) => item.id === this.currentId);
      return current ? current.name || "未命名会话" : "";
    }
  },
  methods: {
    handleToggle() {
      this.$emit("toggle");
    },
    handleSelect(conversation) {
      this.$emit("select", conversation);
      this.$emit("toggle");
    },
    formatTime(timestamp) {
      if (!timestamp) return "";
      const date = new Date(timestamp * 1000);
      const now = new Date();
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
      const diffDays = Math.floor((today - targetDate) / (1000 * 60 * 60 * 24));

      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");

      if (diffDays === 0) {
        return `今天 ${hours}:${minutes}`;
      } else if (diffDays === 1) {
        return `昨天 ${hours}:${minutes}`;
      } else if (diffDays < 7) {
        return `${diffDays}天前 ${hours}:${minutes}`;
      } else {
        return `${month}-${day} ${hours}:${minutes}`;
      }
    }
  }
};
</script>

<style scoped>
.selector-wrapper {
  position: relative;
  width: 100%;
}

.selector-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 26rpx;
  transition: all 0.2s;
  backdrop-filter: blur(10rpx);
}

.selector-trigger:active {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(0.98);
}

.trigger-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.trigger-arrow {
  font-size: 20rpx;
  transition: transform 0.3s ease;
  opacity: 0.8;
}

.trigger-arrow.open {
  transform: rotate(180deg);
}

.selector-dropdown {
  position: absolute;
  top: calc(100% + 12rpx);
  left: 0;
  right: 0;
  width: 100%;
  max-height: 500rpx;
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
  z-index: 1000;
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-loading,
.dropdown-empty {
  padding: 60rpx 40rpx;
  text-align: center;
  color: #a0aec0;
  font-size: 28rpx;
}

.dropdown-list {
  max-height: 500rpx;
}

.dropdown-item {
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f1f5f9;
  transition: all 0.2s;
  position: relative;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:active {
  background: #f7fafc;
}

.dropdown-item.active {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.dropdown-item.active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4rpx;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
}

.item-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.item-time {
  font-size: 24rpx;
  color: #718096;
}

.selector-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.3);
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>

