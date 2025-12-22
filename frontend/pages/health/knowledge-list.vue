<template>
  <view class="container">
    <view class="item" v-for="item in list" :key="item.id">
      <view class="title">{{ item.title }}</view>
      <view class="desc">{{ item.desc }}</view>
    </view>

    <view class="state" v-if="loading">加载中...</view>
    <view class="state" v-else-if="list.length === 0">暂无数据</view>
  </view>
</template>

<script>
// =========================
// ✅ Mock 开关：手动改 true / false（无 UI）
// true  = 使用假数据（前端开发）
// false = 使用真实接口（联调/后端）
// =========================
const USE_MOCK = false;

// ✅ 后端地址：电脑运行用 127.0.0.1；手机/模拟器要改成你电脑局域网 IP
const BASE_URL = "http://127.0.0.1:8080";

// ✅ 接口按文档：GET /api/v1/articles  :contentReference[oaicite:2]{index=2}
const API_URL = `${BASE_URL}/api/v1/articles`;

// ✅ 假数据：字段保持 id/title/desc（只给前端看样式）
const MOCK_LIST = [
  { id: 1, title: "SDG3：确保健康的生活方式", desc: "促进所有年龄段人群的福祉。" },
  { id: 2, title: "合理用药的重要性", desc: "按时、按量、按医嘱服药是健康管理关键。" },
  { id: 3, title: "心理健康同样重要", desc: "关注情绪变化，必要时寻求专业帮助。" }
];

// ✅ 后端字段映射：
// 文档没强制文章字段名，所以这里做“兼容映射”，但最终仍输出 {id,title,desc}
// 后端若能直接返回 {id,title,desc} 最省事
function mapApiItem(raw) {
  return {
    id: raw.id,
    // 常见文章标题字段：title / name
    title: raw.title ?? raw.name ?? "",
    // 常见摘要字段：desc / description / summary / content
    // 注意：若后端只有 content（正文很长），建议后端再给一个 summary
    desc: raw.desc ?? raw.description ?? raw.summary ?? raw.content ?? ""
  };
}

export default {
  data() {
    return {
      list: [],
      loading: false
    };
  },

  onLoad() {
    this.initData();
  },

  methods: {
    initData() {
      if (USE_MOCK) {
        // ✅ Mock 模式：完全不请求后端
        this.loading = false;
        this.list = MOCK_LIST;
        return;
      }

      // ✅ 真实模式：只请求后端
      this.fetchList();
    },

    fetchList() {
      this.loading = true;

      uni.request({
        url: API_URL,
        method: "GET",
        // ✅ 文档：除 login/captcha 外都需要 Bearer Token :contentReference[oaicite:3]{index=3}
        // 如果你项目里有封装好的 request.js（自动带 token），建议用封装；否则这里手动带：
        header: {
          Authorization: `Bearer ${uni.getStorageSync("token") || ""}`
        },
        // 可选 query：category/keyword/page/size（文档里写了 query 参数） :contentReference[oaicite:4]{index=4}
        // data: { category:"", keyword:"", page:1, size:10 },

        success: (res) => {
          // ✅ 统一响应：成功 {code:0,msg:"ok",data:<payload>} :contentReference[oaicite:5]{index=5}
          const body = res.data || {};
          if (body.code !== 0) {
            this.list = [];
            uni.showToast({ title: body.msg || "接口返回错误", icon: "none" });
            return;
          }

          // 文章列表通常是 data=[...]
          const arr = Array.isArray(body.data) ? body.data : [];
          this.list = arr.map(mapApiItem).filter((x) => x.id != null);
        },

        fail: (err) => {
          console.log("接口请求失败：", err);
          // ✅ 真实模式：失败也不回退 mock，直接空
          this.list = [];
          uni.showToast({ title: "接口请求失败", icon: "none" });
        },

        complete: () => {
          this.loading = false;
        }
      });
    }
  }
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}
.item {
  background: #fff;
  padding: 24rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}
.title {
  font-weight: bold;
  font-size: 30rpx;
}
.desc {
  color: #666;
  margin-top: 10rpx;
}
.state {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
}
</style>
