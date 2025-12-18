<template>
  <view class="page">
    <view class="card">
      <view class="row head">
        <text class="title">用药计划</text>
        <button class="mini primary" @click="goCreate">+ 新建</button>
      </view>

      <view v-if="loading" class="muted">加载中...</view>
      <view v-else-if="!list.length" class="muted">暂无计划，先新建一个吧。</view>

      <view v-for="p in list" :key="p.id" class="item" @click="goDetail(p.id)">
        <view class="row">
          <view>
            <view class="name">{{ p.name }}</view>
            <view class="sub">
              剂量：{{ p.dosage }} ｜ 时间：{{ formatTimes(p.times) }}
            </view>
            <view class="sub">
              周期：{{ formatRepeat(p) }} ｜ {{ p.startDate }} ~ {{ p.endDate }}
            </view>
          </view>
          <view class="right">
            <text class="tag" :class="p.remindEnabled ? 'on' : 'off'">
              {{ p.remindEnabled ? "已提醒" : "未提醒" }}
            </text>
          </view>
        </view>

        <view class="row actions" @click.stop>
          <button class="mini" @click="goEdit(p.id)">编辑</button>
          <button class="mini danger" @click="onDelete(p.id)">删除</button>
        </view>
      </view>

      <view class="row foot">
        <button class="mini" @click="goToday">今日用药</button>
        <button class="mini" @click="goRecords">用药记录</button>
        <button class="mini" @click="goSubscribe">开启提醒</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

// ✅ 开关：后端没做好就 true；后端好了改成 false
const USE_MOCK = true;

// ✅ 初始假数据（你可以随便改）
const DEFAULT_MOCK_PLANS = [
  {
    id: "m1",
    name: "阿莫西林",
    dosage: "0.5g / 次",
    times: ["08:00", "13:00", "20:00"],
    repeatType: "daily",
    repeatDays: [],
    startDate: "2025-12-18",
    endDate: "2025-12-25",
    remindEnabled: true
  },
  {
    id: "m2",
    name: "维生素D",
    dosage: "1粒 / 次",
    times: ["09:00"],
    repeatType: "weekly",
    repeatDays: [1, 3, 5], // 周一三五
    startDate: "2025-12-01",
    endDate: "2026-01-31",
    remindEnabled: false
  }
];

const MOCK_KEY = "mock_med_plans";

function loadMockPlans() {
  const saved = uni.getStorageSync(MOCK_KEY);
  if (Array.isArray(saved) && saved.length) return saved;
  uni.setStorageSync(MOCK_KEY, DEFAULT_MOCK_PLANS);
  return DEFAULT_MOCK_PLANS;
}
function saveMockPlans(list) {
  uni.setStorageSync(MOCK_KEY, list);
}

export default {
  data() {
    return { loading: false, list: [] };
  },
  onShow() {
    this.fetchList();
  },
  methods: {
    async fetchList() {
      this.loading = true;
      try {
        // ✅ mock 模式：不调接口，直接用本地数据
        if (USE_MOCK) {
          this.list = loadMockPlans();
          return;
        }

        // ✅ 正常模式：走后端接口
        const data = await request({ url: API.medPlans, method: "GET" });
        this.list = Array.isArray(data) ? data : (data?.list || []);
      } finally {
        this.loading = false;
      }
    },

    // （你原来的展示方法保留）
    formatTimes(times) {
      if (Array.isArray(times)) return times.join("、");
      if (typeof times === "string") return times;
      return "-";
    },
    formatRepeat(p) {
      if (p.repeatType === "weekly") {
        const map = ["日","一","二","三","四","五","六"];
        const days = (p.repeatDays || []).map(d => map[d % 7]).join("、");
        return `每周(${days || "-"})`;
      }
      return "每日";
    },

    // ✅ mock 下：点“+ 新建”就直接本地加一条，方便你马上看效果
    goCreate() {
      if (!USE_MOCK) {
        uni.navigateTo({ url: "/pages/med/plan-edit" });
        return;
      }

      const newPlan = {
        id: "m" + Date.now(),
        name: "新计划（示例）",
        dosage: "1片 / 次",
        times: ["08:30", "21:30"],
        repeatType: "daily",
        repeatDays: [],
        startDate: "2025-12-18",
        endDate: "2026-01-18",
        remindEnabled: true
      };

      const next = [newPlan, ...(this.list || [])];
      this.list = next;
      saveMockPlans(next);
      uni.showToast({ title: "已本地新增(假数据)", icon: "none" });
    },

    goEdit(id) {
      uni.navigateTo({ url: `/pages/med/plan-edit?id=${id}` });
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/med/plan-detail?id=${id}` });
    },
    goToday() {
      uni.navigateTo({ url: "/pages/med/today-checkin" });
    },
    goRecords() {
      uni.navigateTo({ url: "/pages/med/record-list" });
    },
    goSubscribe() {
      uni.navigateTo({ url: "/pages/med/subscribe-guide" });
    },

    // ✅ 删除：mock 模式下走本地删除；正常模式走后端 DELETE
    onDelete(id) {
      uni.showModal({
        title: "确认删除",
        content: "删除后不可恢复",
        success: async (res) => {
          if (!res.confirm) return;

          if (USE_MOCK) {
            const next = (this.list || []).filter(p => p.id !== id);
            this.list = next;
            saveMockPlans(next);
            uni.showToast({ title: "已本地删除(假数据)", icon: "none" });
            return;
          }

          await request({ url: API.medPlanDetail(id), method: "DELETE" });
          uni.showToast({ title: "已删除", icon: "success" });
          this.fetchList();
        }
      });
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
.item{padding:22rpx 0;border-top:1rpx solid #eef0f3;}
.name{font-size:32rpx;font-weight:600;color:#111;}
.sub{margin-top:8rpx;font-size:24rpx;color:#8a8f99;}
.right{display:flex;flex-direction:column;align-items:flex-end;gap:10rpx;}
.tag{font-size:22rpx;padding:8rpx 14rpx;border-radius:999rpx;}
.tag.on{background:#e8f8ef;color:#07c160;}
.tag.off{background:#f2f3f5;color:#666;}
.actions{margin-top:14rpx;justify-content:flex-end;}
.foot{margin-top:18rpx;justify-content:space-between;flex-wrap:wrap;}
.muted{color:#8a8f99;padding:18rpx 0;}
.mini{height:64rpx;line-height:64rpx;font-size:26rpx;border-radius:14rpx;margin:0;}
.primary{background:#07c160;color:#fff;}
.danger{background:#ff4d4f;color:#fff;}
button::after{border:none;}
</style>
