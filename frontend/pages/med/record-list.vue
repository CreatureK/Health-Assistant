<template>
  <view class="page">
    <view class="card">
      <view class="title">用药记录</view>

      <view class="filters">
        <picker mode="selector" :range="statusOptionsText" :value="statusIndex" @change="onPickStatus">
          <view class="pick">状态：{{ statusOptionsText[statusIndex] }}</view>
        </picker>

        <picker mode="date" :value="startDate" @change="e=>{startDate=e.detail.value;fetch()}">
          <view class="pick">开始：{{ startDate }}</view>
        </picker>
        <picker mode="date" :value="endDate" @change="e=>{endDate=e.detail.value;fetch()}">
          <view class="pick">结束：{{ endDate }}</view>
        </picker>
      </view>

      <view v-if="loading" class="muted">加载中...</view>
      <view v-else-if="!list.length" class="muted">暂无记录</view>

      <view v-for="r in list" :key="r.id" class="item">
        <view class="name">{{ r.planName || r.name }}</view>
        <view class="sub">时间：{{ r.actionAt || r.time || "-" }} ｜ 剂量：{{ r.dosage }}</view>
        <view class="sub">状态：{{ statusText(r.status) }}</view>

        <view class="row actions">
          <button class="mini" @click="goAdjust(r.id)">补记/更正</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

function todayStr(offsetDays=0){
  const d = new Date();
  d.setDate(d.getDate()+offsetDays);
  const y=d.getFullYear(), m=String(d.getMonth()+1).padStart(2,'0'), dd=String(d.getDate()).padStart(2,'0');
  return `${y}-${m}-${dd}`;
}

export default {
  data() {
    return {
      loading: false,
      list: [],
      planId: "",
      statusOptions: ["all","todo","taken","missed"],
      statusOptionsText: ["全部","待打卡","已服用","未服用"],
      statusIndex: 0,
      startDate: todayStr(-7),
      endDate: todayStr(0)
    };
  },
  onLoad(q) {
    this.planId = q?.planId || "";
    this.fetch();
  },
  onShow() {
    this.fetch();
  },
  methods: {
    statusText(s){
      if (s==="taken") return "已服用";
      if (s==="missed") return "未服用";
      return "待打卡";
    },
    onPickStatus(e){
      this.statusIndex = Number(e.detail.value);
      this.fetch();
    },
    async fetch() {
      this.loading = true;
      try {
        const status = this.statusOptions[this.statusIndex];
        const data = await request({
          url: API.medRecords,
          method: "GET",
          data: {
            planId: this.planId || undefined,
            status: status === "all" ? undefined : status,
            startDate: this.startDate,
            endDate: this.endDate
          }
        });
        this.list = Array.isArray(data) ? data : (data?.list || []);
      } finally {
        this.loading = false;
      }
    },
    goAdjust(id){
      uni.navigateTo({ url: `/pages/med/record-adjust?recordId=${id}` });
    }
  }
};
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f7f8fa;}
.card{background:#fff;border-radius:24rpx;padding:28rpx;box-shadow:0 10rpx 30rpx rgba(0,0,0,.06);}
.title{font-size:40rpx;font-weight:700;margin-bottom:14rpx;}
.filters{display:flex;gap:14rpx;flex-wrap:wrap;margin-bottom:10rpx;}
.pick{padding:12rpx 16rpx;border-radius:12rpx;background:#f6f7f9;font-size:24rpx;color:#111;}
.item{padding:22rpx 0;border-top:1rpx solid #eef0f3;}
.name{font-size:32rpx;font-weight:600;color:#111;}
.sub{margin-top:8rpx;font-size:24rpx;color:#666;}
.row{display:flex;justify-content:flex-end;margin-top:12rpx;}
.mini{height:64rpx;line-height:64rpx;font-size:26rpx;border-radius:14rpx;margin:0;}
button::after{border:none;}
.muted{color:#8a8f99;padding:18rpx 0;}
</style>
