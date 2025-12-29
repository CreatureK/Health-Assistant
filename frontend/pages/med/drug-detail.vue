<template>
  <view class="container">
    <view v-if="loading" class="hint">加载中...</view>

    <view v-else>
      <view class="header">
        <view class="name">{{ detail.name || "-" }}</view>
        <view v-if="detail.commonNames && detail.commonNames.length" class="alias">
          常见名：{{ detail.commonNames.join("、") }}
        </view>
      </view>

      <view class="section" v-if="detail.intro">
        <view class="stitle">通俗说明</view>
        <view class="scontent">{{ detail.intro }}</view>
      </view>

      <view class="section" v-if="detail.usage">
        <view class="stitle">一般如何使用</view>
        <view class="scontent">{{ detail.usage }}</view>
      </view>

      <view class="section" v-if="detail.warnings">
        <view class="stitle">注意事项</view>
        <view class="scontent">{{ detail.warnings }}</view>
      </view>

      <view class="disclaimer" v-if="detail.disclaimer">
        {{ detail.disclaimer }}
      </view>

      <view v-if="!detail || !detail.id" class="empty">暂无详情</view>
    </view>
  </view>
</template>

<script>
import { request } from "@/common/request.js";
import { API } from "@/common/api.js";

export default {
  data() {
    return {
      id: "",
      loading: false,
      detail: {}
    };
  },
  onLoad(query) {
    this.id = query?.id ? String(query.id) : "";
    if (!this.id) {
      uni.showToast({ title: "缺少药品ID", icon: "none" });
      return;
    }
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      if (this.loading) return;
      this.loading = true;
      try {
        this.detail = await request({
          url: API.medDrugDetail(encodeURIComponent(this.id)),
          method: "GET"
        });
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.container { padding: 24rpx 24rpx 50rpx; }
.header {
  background: #fff; padding: 28rpx; border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.06); margin-bottom: 18rpx;
}
.name { font-size: 38rpx; font-weight: 800; }
.alias { margin-top: 12rpx; color: #666; font-size: 26rpx; }
.section {
  background: #fff; padding: 26rpx; border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.06); margin-bottom: 18rpx;
}
.stitle { font-size: 30rpx; font-weight: 700; margin-bottom: 12rpx; }
.scontent { color: #333; font-size: 28rpx; line-height: 44rpx; }
.disclaimer {
  margin-top: 10rpx; padding: 22rpx 20rpx; border-radius: 16rpx;
  background: #fff7f7; color: #b42318; font-size: 26rpx; line-height: 40rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.04);
}
.empty, .hint { text-align: center; color: #888; padding: 80rpx 0; }
</style>
