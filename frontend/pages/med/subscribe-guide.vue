<template>
  <view class="page">
    <view class="card">
      <view class="title">开启提醒</view>
      <view class="sub">用于“到点自动推送微信订阅消息提醒”（首次需授权）。</view>

      <view class="steps">
        <view class="step">1）点击下方按钮申请订阅</view>
        <view class="step">2）选择“允许”</view>
        <view class="step">3）回到计划详情开启“微信提醒”</view>
      </view>

      <button class="btn primary" :disabled="loading" @click="doSubscribe">
        {{ loading ? "处理中..." : "申请订阅提醒" }}
      </button>

      <view class="tips">
        <text>注：仅微信小程序环境可用；H5/APP 可能不支持订阅消息API。</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

export default {
  data() {
    return { loading: false, templateIds: [] };
  },
  onShow() {
    this.fetchConfig();
  },
  methods: {
    async fetchConfig() {
      try {
        const data = await request({ url: API.wechatSubscribeConfig, method: "GET" });
        this.templateIds = data?.templateIds || [];
      } catch (e) {
        this.templateIds = [];
      }
    },
    async doSubscribe() {
      // 仅微信小程序支持
      // #ifdef MP-WEIXIN
      if (!this.templateIds.length) {
        uni.showToast({ title: "未获取到模板ID", icon: "none" });
        return;
      }

      this.loading = true;
      try {
        const res = await new Promise((resolve) => {
          uni.requestSubscribeMessage({
            tmplIds: this.templateIds,
            complete: resolve
          });
        });

        // res 里每个模板会返回 "accept"/"reject"/"ban"
        const granted = Object.values(res || {}).some(v => v === "accept");

        await request({
          url: API.wechatSubscribeReport,
          method: "POST",
          data: { granted, detail: res }
        });

        uni.showToast({ title: granted ? "已授权" : "未授权", icon: "none" });
      } finally {
        this.loading = false;
      }
      // #endif

      // #ifndef MP-WEIXIN
      uni.showToast({ title: "请在微信小程序中操作", icon: "none" });
      // #endif
    }
  }
};
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f7f8fa;}
.card{background:#fff;border-radius:24rpx;padding:28rpx;box-shadow:0 10rpx 30rpx rgba(0,0,0,.06);}
.title{font-size:40rpx;font-weight:700;margin-bottom:10rpx;}
.sub{font-size:26rpx;color:#8a8f99;line-height:1.6;margin-bottom:18rpx;}
.steps{background:#f6f7f9;border-radius:18rpx;padding:18rpx;margin-bottom:18rpx;}
.step{font-size:26rpx;color:#333;line-height:1.8;}
.btn{margin-top:10rpx;height:92rpx;line-height:92rpx;border-radius:18rpx;font-size:32rpx;font-weight:600;}
.primary{background:#07c160;color:#fff;}
button::after{border:none;}
.tips{margin-top:16rpx;color:#9aa0a6;font-size:24rpx;line-height:1.6;}
</style>
