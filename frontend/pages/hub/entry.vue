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

    <!-- 替换：原“健康日记”入口删除，改为“药品库” -->
    <view class="card" @click="go('/pages/med/drug-list')">
      🧾 药品库
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    go(url) {
      uni.navigateTo({ url });
    }
  }
};
</script>

<style scoped>
.container {
  padding: 40rpx 32rpx;
}
.title {
  font-size: 44rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
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
