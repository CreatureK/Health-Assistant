<template>
  <view class="page">
    <view class="card">
      <view class="title">补记 / 更正</view>

      <view v-if="loading" class="muted">加载中...</view>

      <view v-else>
        <view class="sub">记录ID：{{ recordId }}</view>

        <view class="field">
          <text class="label">状态</text>
          <radio-group @change="e=>form.status=e.detail.value">
            <label class="radio"><radio value="taken" :checked="form.status==='taken'"/>已服用</label>
            <label class="radio"><radio value="missed" :checked="form.status==='missed'"/>未服用</label>
          </radio-group>
        </view>

        <view class="field">
          <text class="label">发生时间</text>
          <picker mode="date" :value="date" @change="e=>{date=e.detail.value}">
            <view class="pick">{{ date }}</view>
          </picker>
          <picker mode="time" :value="time" @change="e=>{time=e.detail.value}">
            <view class="pick">{{ time }}</view>
          </picker>
        </view>

        <view class="field">
          <text class="label">备注</text>
          <input class="input" v-model.trim="form.note" placeholder="例如：忘记打卡，已补记" />
        </view>

        <button class="btn primary" :disabled="saving" @click="submit">
          {{ saving ? "提交中..." : "提交更正" }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

function todayStr(){
  const d = new Date();
  const y=d.getFullYear(), m=String(d.getMonth()+1).padStart(2,'0'), dd=String(d.getDate()).padStart(2,'0');
  return `${y}-${m}-${dd}`;
}
function nowTime(){
  const d = new Date();
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

export default {
  data() {
    return {
      recordId: "",
      loading: false,
      saving: false,
      date: todayStr(),
      time: nowTime(),
      form: { status: "taken", note: "" }
    };
  },
  onLoad(q) {
    this.recordId = q?.recordId || "";
    this.fetchDetail();
  },
  methods: {
    async fetchDetail() {
      if (!this.recordId) return;
      this.loading = true;
      try {
        // 通过 records 接口用 id 查询（契约里支持 id?）
        const data = await request({
          url: API.medRecords,
          method: "GET",
          data: { id: this.recordId }
        });
        const rec = Array.isArray(data) ? data[0] : (data?.list?.[0] || data);
        if (rec?.status) this.form.status = rec.status;
        if (rec?.note) this.form.note = rec.note;
        if (rec?.actionAt) {
          // actionAt 形如 2025-01-01 08:00 或 ISO，都尽量拆
          const s = String(rec.actionAt);
          const m = s.match(/^(\d{4}-\d{2}-\d{2}).*?(\d{2}:\d{2})/);
          if (m) { this.date = m[1]; this.time = m[2]; }
        }
      } finally {
        this.loading = false;
      }
    },
    async submit() {
      if (!this.recordId) return uni.showToast({ title: "缺少recordId", icon: "none" });

      this.saving = true;
      try {
		  // ✅ 1) 先拼 actionAt
		  let actionAt = `${this.date}T${this.time}`;
	  
		  // ✅ 2) 补秒，变成 yyyy-MM-dd HH:mm:ss
		  if (actionAt.length === 16) actionAt = actionAt + ":00";
        await request({
          url: API.medRecordAdjust(this.recordId),
          method: "POST",
          data: {
            status: this.form.status,
            actionAt,
            note: this.form.note
          }
        });
        uni.showToast({ title: "已提交", icon: "success" });
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
.title{font-size:40rpx;font-weight:700;margin-bottom:10rpx;}
.sub{margin-top:6rpx;font-size:24rpx;color:#8a8f99;}
.field{padding:18rpx 0;border-bottom:1rpx solid #eef0f3;}
.label{font-size:26rpx;color:#222;display:block;margin-bottom:12rpx;}
.input{height:84rpx;padding:0 18rpx;font-size:30rpx;background:#f6f7f9;border-radius:16rpx;box-sizing:border-box;}
.pick{display:inline-block;margin-right:14rpx;padding:12rpx 16rpx;border-radius:12rpx;background:#f6f7f9;font-size:24rpx;color:#111;}
.radio{margin-right:22rpx;font-size:28rpx;color:#111;}
.btn{margin-top:26rpx;height:92rpx;line-height:92rpx;border-radius:18rpx;font-size:32rpx;font-weight:600;}
.primary{background:#07c160;color:#fff;}
button::after{border:none;}
.muted{color:#8a8f99;padding:18rpx 0;}
</style>
