<template>
  <view class="page">
    <view class="card">
      <view class="title">
        用药记录
        <text v-if="mocking" class="tag">示例数据</text>
      </view>

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

      <!-- 可选：一键切换模式（你不需要就删掉这块） -->
      <!-- <view class="row toggle-row">
        <button class="mini" @click="toggleMode">
          {{ useRealOnly ? "当前：真实接口（点我切前端示例模式）" : "当前：前端示例模式（点我切真实接口）" }}
        </button>
      </view> -->

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
import { request } from "@/common/request";
import { API } from "@/common/api";

function todayStr(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const y = d.getFullYear(),
    m = String(d.getMonth() + 1).padStart(2, "0"),
    dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}

function pad2(n) {
  return String(n).padStart(2, "0");
}
function timeStr(offsetMin = 0) {
  const d = new Date(Date.now() + offsetMin * 60 * 1000);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

// 示例记录（会根据筛选状态做过滤）
function mockRecords({ status, planId }) {
  const base = [
    { id: "mock-1", planId: planId || "p1", planName: "阿莫西林", dosage: "1粒", status: "todo",   time: timeStr(15),   actionAt: "" },
    { id: "mock-2", planId: planId || "p2", planName: "维生素C", dosage: "1片", status: "taken",  time: timeStr(-60),  actionAt: todayStr(0) + " " + timeStr(-60) },
    { id: "mock-3", planId: planId || "p3", planName: "布洛芬",  dosage: "1粒", status: "missed",time: timeStr(-180), actionAt: todayStr(0) + " " + timeStr(-180) },
  ];
  if (!status || status === "all") return base;
  return base.filter((x) => x.status === status);
}

export default {
  data() {
    return {
      loading: false,
      list: [],
      planId: "",

      statusOptions: ["all", "todo", "taken", "missed"],
      statusOptionsText: ["全部", "待打卡", "已服用", "未服用"],
      statusIndex: 0,

      startDate: todayStr(-7),
      endDate: todayStr(0),

      mocking: false,

      // ✅ 你期望的开关：
      // true：不显示假数据（后端联调模式）
      // false：接口无数据/报错时显示假数据（前端做样式布局）
      useRealOnly: false,
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
    toggleMode() {
      this.useRealOnly = !this.useRealOnly;
      this.fetch();
    },
    statusText(s) {
      if (s === "taken") return "已服用";
      if (s === "missed") return "未服用";
      return "待打卡";
    },
    onPickStatus(e) {
      this.statusIndex = Number(e.detail.value);
      this.fetch();
    },

    async fetch() {
      this.loading = true;
      this.mocking = false;

      const status = this.statusOptions[this.statusIndex];

      try {
        const data = await request({
          url: API.medRecords,
          method: "GET",
          data: {
            planId: this.planId || undefined,
            status: status === "all" ? undefined : status,
            startDate: this.startDate,
            endDate: this.endDate,
          },
        });

        const arr = Array.isArray(data) ? data : (data?.list || []);

        // ✅ 真实接口有数据：直接展示
        if (arr && arr.length) {
          this.list = arr;
          return;
        }

        // ✅ 真实接口没数据：
        // - 后端联调模式(useRealOnly=true)：不兜底，保持空列表
        // - 前端示例模式(useRealOnly=false)：塞 mock
        if (this.useRealOnly) {
          this.list = [];
        } else {
          this.list = mockRecords({ status, planId: this.planId });
          this.mocking = true;
        }
      } catch (err) {
        // ✅ 请求报错：
        // - 后端联调模式：不兜底，保持空列表（你也可以在这里加 toast）
        // - 前端示例模式：塞 mock
        if (this.useRealOnly) {
          this.list = [];
        } else {
          this.list = mockRecords({ status, planId: this.planId });
          this.mocking = true;
        }
      } finally {
        this.loading = false;
      }
    },

    goAdjust(id) {
      // 不想 mock 记录跳转的话可打开这段
      // if (String(id).startsWith("mock-")) {
      //   uni.showToast({ title: "示例数据无法补记", icon: "none" });
      //   return;
      // }
      uni.navigateTo({ url: `/pages/med/record-adjust?recordId=${id}` });
    },
  },
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f8fa;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.06);
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.tag {
  font-size: 22rpx;
  color: #6b7280;
  background: #f3f4f6;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
}

.filters {
  display: flex;
  gap: 14rpx;
  flex-wrap: wrap;
  margin-bottom: 10rpx;
}
.pick {
  padding: 12rpx 16rpx;
  border-radius: 12rpx;
  background: #f6f7f9;
  font-size: 24rpx;
  color: #111;
}

.row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12rpx;
}
.toggle-row {
  margin-top: 4rpx;
  margin-bottom: 6rpx;
  justify-content: flex-start;
}

.item {
  padding: 22rpx 0;
  border-top: 1rpx solid #eef0f3;
}
.name {
  font-size: 32rpx;
  font-weight: 600;
  color: #111;
}
.sub {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #666;
}
.actions {
  justify-content: flex-end;
  margin-top: 12rpx;
}

.mini {
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  border-radius: 14rpx;
  margin: 0;
}
button::after {
  border: none;
}
.muted {
  color: #8a8f99;
  padding: 18rpx 0;
}
</style>
