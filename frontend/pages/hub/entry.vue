<template>
  <view class="container">
    <view class="title">欢迎 👋</view>
    <view class="subtitle">请选择你要进入的功能</view>

    <view class="card" @click="go('/pages/med/plan-list')">
      💊 用药计划管理
    </view>

    <view class="card" @click="go('/pages/health/knowledge-list')">
      📚 健康知识（SDG3）
    </view>

    <view class="card" @click="go('/pages/health/ai-chat')">
      🤖 AI 健康对话助手
    </view>

    <view class="card" @click="go('/pages/health/diary')">
      📝 健康日记（手动记录）
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    go(url) {
      // ✅ plan-list 是启动页/主页面时，navigateTo 可能不生效，用 reLaunch 最稳
      if (url === "/pages/med/plan-list") {
        uni.reLaunch({ url });
        return;
      }
      uni.navigateTo({ url });
    }
  }
};
</script>

<style scoped>
.container {
  padding: 40rpx;
}
.title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}
.subtitle {
  color: #666;
  margin-bottom: 40rpx;
}
.card {
  background: #ffffff;
  padding: 30rpx;
  margin-bottom: 24rpx;
  border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
  font-size: 30rpx;
}
</style>
