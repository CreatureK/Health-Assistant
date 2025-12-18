<template>
  <view class="page">
    <view class="card" v-if="plan">
      <view class="title">{{ plan.name }}</view>
      <view class="sub">剂量：{{ plan.dosage }}</view>
      <view class="sub">时间：{{ (plan.times||[]).join("、") }}</view>
      <view class="sub">周期：{{ repeatText }}</view>
      <view class="sub">日期：{{ plan.startDate }} ~ {{ plan.endDate }}</view>

      <view class="line"></view>

      <view class="row">
        <text class="label">微信提醒</text>
        <switch :checked="!!plan.remindEnabled" @change="onToggleRemind"/>
      </view>

      <view class="row actions">
        <button class="mini" @click="goEdit">编辑</button>
        <button class="mini" @click="goRecords">查看记录</button>
        <button class="mini primary" @click="goToday">今日打卡</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

export default {
  data() {
    return { id: "", plan: null };
  },
  computed: {
    repeatText() {
      const p = this.plan || {};
      if (p.repeatType === "weekly") {
        const map = ["日","一","二","三","四","五","六"];
        const days = (p.repeatDays || []).map(d => map[d % 7]).join("、");
        return `每周：${days || "-"}`;
      }
      return "每日";
    }
  },
  onLoad(q) {
    this.id = q?.id || "";
    this.fetchDetail();
  },
  onShow() {
    if (this.id) this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      const data = await request({ url: API.medPlanDetail(this.id), method: "GET" });
      this.plan = data || null;
    },
    async onToggleRemind(e) {
      const v = !!e.detail.value;
      await request({
        url: API.medPlanRemind(this.id),
        method: "POST",
        data: { remindEnabled: v }
      });
      this.plan.remindEnabled = v;
      uni.showToast({ title: v ? "已开启提醒" : "已关闭提醒", icon: "none" });
      if (v) uni.navigateTo({ url: "/pages/med/subscribe-guide" });
    },
    goEdit() {
      uni.navigateTo({ url: `/pages/med/plan-edit?id=${this.id}` });
    },
    goToday() {
      uni.navigateTo({ url: "/pages/med/today-checkin" });
    },
    goRecords() {
      uni.navigateTo({ url: `/pages/med/record-list?planId=${this.id}` });
    }
  }
};
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f7f8fa;}
.card{background:#fff;border-radius:24rpx;padding:28rpx;box-shadow:0 10rpx 30rpx rgba(0,0,0,.06);}
.title{font-size:40rpx;font-weight:700;margin-bottom:10rpx;}
.sub{margin-top:8rpx;font-size:26rpx;color:#666;}
.line{height:1rpx;background:#eef0f3;margin:20rpx 0;}
.row{display:flex;align-items:center;justify-content:space-between;}
.label{font-size:28rpx;color:#111;}
.actions{margin-top:18rpx;gap:14rpx;flex-wrap:wrap;justify-content:flex-end;}
.mini{height:64rpx;line-height:64rpx;font-size:26rpx;border-radius:14rpx;margin:0;}
.primary{background:#07c160;color:#fff;}
button::after{border:none;}
</style>
