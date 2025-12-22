<template>
  <view class="page">
    <view class="card">
      <view class="row head">
        <view class="head-left">
          <text class="title">今日用药</text>
          <!-- <text v-if="isMock" class="badge">示例数据</text> -->
        </view>
        <picker mode="date" :value="date" @change="onPickDate">
          <view class="pick">{{ date }}</view>
        </picker>
      </view>

      <view v-if="loading" class="muted">加载中...</view>
      <view v-else-if="!records.length" class="muted">今天没有需要打卡的记录。</view>

      <view v-for="r in records" :key="r.id" class="item">
        <view class="name">{{ r.planName || r.name }}</view>
        <view class="sub">时间：{{ r.time }} ｜ 剂量：{{ r.dosage }}</view>
        <view class="sub">状态：<text :class="statusClass(r.status)">{{ statusText(r.status) }}</text></view>

        <view class="row actions">
          <button class="mini primary" @click="mark(r,'taken')">已服用</button>
          <button class="mini danger" @click="mark(r,'missed')">未服用</button>
          <button class="mini" @click="goAdjust(r.id)">补记/更正</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from "@/common/request";
import { API } from "@/common/api";

function todayStr() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth()+1).padStart(2,"0");
  const dd = String(d.getDate()).padStart(2,"0");
  return `${y}-${m}-${dd}`;
}


function mockRecords(date) {
  // 用于前端自测布局：后端无数据时展示
  return [
    { id: `${date}-1`, planName: "阿司匹林", time: "08:00", dosage: "1片", status: "todo" },
    { id: `${date}-2`, planName: "维生素D", time: "12:30", dosage: "1粒", status: "taken" },
    { id: `${date}-3`, planName: "二甲双胍", time: "19:00", dosage: "0.5片", status: "missed" }
  ];
}

export default {
  data() {
    return { date: todayStr(), loading: false, records: [], isMock: false };
  },
  onShow() {
    this.fetchToday();
  },
  methods: {
    onPickDate(e) {
      this.date = e.detail.value;
      this.fetchToday();
    },
    async fetchToday() {
      this.loading = true;
      try {
        const data = await request({
          url: API.medToday,
          method: "GET",
          data: { date: this.date }
        });
        const list = Array.isArray(data) ? data : (data?.list || []);
        this.isMock = !list.length;
        this.records = list.length ? list : mockRecords(this.date);
      } catch (e) {
        // 网络/后端异常时也用示例数据，方便调试页面
        this.isMock = true;
        this.records = mockRecords(this.date);
      } finally {
        this.loading = false;
      }
    },
    statusText(s) {
      if (s === "taken") return "已服用";
      if (s === "missed") return "未服用";
      return "待打卡";
    },
    statusClass(s) {
      if (s === "taken") return "ok";
      if (s === "missed") return "bad";
      return "todo";
    },
    async mark(r, status) {
      await request({
        url: API.medRecordMark(r.id),
        method: "POST",
        data: { status } // taken|missed
      });
      r.status = status;
      uni.showToast({ title: "已记录", icon: "success" });
    },
    goAdjust(recordId) {
      uni.navigateTo({ url: `/pages/med/record-adjust?recordId=${recordId}` });
    }
  }
};
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f7f8fa;}
.card{background:#fff;border-radius:24rpx;padding:28rpx;box-shadow:0 10rpx 30rpx rgba(0,0,0,.06);}
.row{display:flex;align-items:center;justify-content:space-between;gap:18rpx;}
.head{margin-bottom:18rpx;}
.title{font-size:40rpx;font-weight:700;}
.pick{padding:10rpx 16rpx;border-radius:12rpx;background:#f6f7f9;font-size:24rpx;color:#111;}
.item{padding:22rpx 0;border-top:1rpx solid #eef0f3;}
.name{font-size:32rpx;font-weight:600;color:#111;}
.sub{margin-top:8rpx;font-size:24rpx;color:#666;}
.actions{margin-top:14rpx;justify-content:flex-end;flex-wrap:wrap;}
.muted{color:#8a8f99;padding:18rpx 0;}
.mini{height:64rpx;line-height:64rpx;font-size:26rpx;border-radius:14rpx;margin:0;}
.primary{background:#07c160;color:#fff;}
.danger{background:#ff4d4f;color:#fff;}
.ok{color:#07c160;}
.bad{color:#ff4d4f;}
.todo{color:#faad14;}
button::after{border:none;}
</style>
