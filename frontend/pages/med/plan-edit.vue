<template>
  <view class="page">
    <view class="card">
      <view class="title">{{ id ? "编辑用药计划" : "新建用药计划" }}</view>

      <view class="field">
        <text class="label">药品名称</text>
        <input class="input" v-model.trim="form.name" placeholder="例如：降压药" />
      </view>

      <view class="field">
        <text class="label">剂量</text>
        <input class="input" v-model.trim="form.dosage" placeholder="例如：1片 / 10ml" />
      </view>

      <view class="field">
        <text class="label">服用时间（多个用逗号分隔）</text>
        <input class="input" v-model.trim="timesText" placeholder="例如：08:00, 20:00" />
      </view>

      <view class="field">
        <text class="label">开始日期</text>
        <picker mode="date" :value="form.startDate" @change="e=>form.startDate=e.detail.value">
          <view class="pick">{{ form.startDate || "选择日期" }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="label">结束日期</text>
        <picker mode="date" :value="form.endDate" @change="e=>form.endDate=e.detail.value">
          <view class="pick">{{ form.endDate || "选择日期" }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="label">重复周期</text>
        <radio-group @change="onRepeatType">
          <label class="radio"><radio value="daily" :checked="form.repeatType==='daily'"/>每日</label>
          <label class="radio"><radio value="weekly" :checked="form.repeatType==='weekly'"/>每周指定</label>
        </radio-group>
      </view>

      <view v-if="form.repeatType==='weekly'" class="field">
        <text class="label">每周哪几天</text>
        <view class="week">
          <view
            v-for="d in weekDays"
            :key="d.value"
            class="chip"
            :class="form.repeatDays.includes(d.value) ? 'on' : ''"
            @click="toggleDay(d.value)"
          >
            {{ d.text }}
          </view>
        </view>
      </view>

      <button class="btn primary" :disabled="saving" @click="onSave">
        {{ saving ? "保存中..." : "保存" }}
      </button>
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

export default {
  data() {
    return {
      id: "",
      saving: false,
      form: {
        name: "",
        dosage: "",
        times: ["08:00"],
        startDate: todayStr(),
        endDate: todayStr(),
        repeatType: "daily", // daily | weekly
        repeatDays: [1,2,3,4,5,6,0], // 周日=0
        remindEnabled: false
      },
      weekDays: [
        { text: "一", value: 1 },{ text: "二", value: 2 },{ text: "三", value: 3 },
        { text: "四", value: 4 },{ text: "五", value: 5 },{ text: "六", value: 6 },{ text: "日", value: 0 }
      ]
    };
  },
  computed: {
    timesText: {
      get() { return (this.form.times || []).join(", "); },
      set(v) {
        const arr = (v || "")
          .split(",")
          .map(s => s.trim())
          .filter(Boolean);
        this.form.times = arr.length ? arr : [];
      }
    }
  },
  onLoad(query) {
    this.id = query?.id || "";
    if (this.id) this.fetchDetail();
  },
  methods: {
    onRepeatType(e) {
      const v = e.detail.value;
      this.form.repeatType = v;
      if (v === "daily") this.form.repeatDays = [1,2,3,4,5,6,0];
      if (v === "weekly" && !this.form.repeatDays?.length) this.form.repeatDays = [1,2,3,4,5];
    },
    toggleDay(val) {
      const s = new Set(this.form.repeatDays || []);
      if (s.has(val)) s.delete(val); else s.add(val);
      this.form.repeatDays = Array.from(s);
    },
    async fetchDetail() {
      const data = await request({ url: API.medPlanDetail(this.id), method: "GET" });
      this.form = {
        ...this.form,
        ...data,
        times: Array.isArray(data?.times) ? data.times : (data?.times ? String(data.times).split(",").map(x=>x.trim()).filter(Boolean) : this.form.times),
        repeatDays: Array.isArray(data?.repeatDays) ? data.repeatDays : this.form.repeatDays
      };
    },
    validate() {
      if (!this.form.name) return "请输入药品名称";
      if (!this.form.dosage) return "请输入剂量";
      if (!this.form.times?.length) return "请输入至少一个服用时间";
      if (!this.form.startDate) return "请选择开始日期";
      if (!this.form.endDate) return "请选择结束日期";
      if (this.form.repeatType === "weekly" && !this.form.repeatDays?.length) return "请选择每周至少一天";
      return "";
    },
    async onSave() {
      const msg = this.validate();
      if (msg) return uni.showToast({ title: msg, icon: "none" });

      this.saving = true;
      try {
        const payload = { ...this.form };
        if (this.id) {
          await request({ url: API.medPlanDetail(this.id), method: "PUT", data: payload });
        } else {
          await request({ url: API.medPlans, method: "POST", data: payload });
        }
        uni.showToast({ title: "已保存", icon: "success" });
        uni.navigateBack();
      } finally {
        this.saving = false;
      }
    }
  }
};
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f7f8fa;}
.card{background:#fff;border-radius:24rpx;padding:28rpx;box-shadow:0 10rpx 30rpx rgba(0,0,0,.06);}
.title{font-size:38rpx;font-weight:700;margin-bottom:20rpx;}
.field{padding:18rpx 0;border-bottom:1rpx solid #eef0f3;}
.label{font-size:26rpx;color:#222;display:block;margin-bottom:12rpx;}
.input{height:84rpx;padding:0 18rpx;font-size:30rpx;background:#f6f7f9;border-radius:16rpx;box-sizing:border-box;}
.pick{height:84rpx;line-height:84rpx;padding:0 18rpx;background:#f6f7f9;border-radius:16rpx;color:#111;}
.radio{margin-right:22rpx;font-size:28rpx;color:#111;}
.week{display:flex;gap:12rpx;flex-wrap:wrap;}
.chip{padding:14rpx 20rpx;border-radius:999rpx;background:#f2f3f5;color:#333;font-size:26rpx;}
.chip.on{background:#e8f8ef;color:#07c160;}
.btn{margin-top:26rpx;height:92rpx;line-height:92rpx;border-radius:18rpx;font-size:32rpx;font-weight:600;}
.primary{background:#07c160;color:#fff;}
button::after{border:none;}
</style>
