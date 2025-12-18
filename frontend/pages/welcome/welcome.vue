<template>
  <view class="page">
    <view class="card">
      <view class="title">欢迎光临</view>

      <view class="row"><text class="k">你的用户账号：</text><text class="v">{{ username }}</text></view>
      <view class="row"><text class="k">你的密码：</text><text class="v">{{ password }}</text></view>
      <view class="row"><text class="k">你的验证码：</text><text class="v">{{ captcha }}</text></view>

      <view class="row"><text class="k">后端返回的 token：</text><text class="v">{{ token }}</text></view>

      <button class="btn" @click="backToLogin">返回登录页</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return { username: "", password: "", captcha: "", token: "" }
  },

  onLoad(query) {
    /**
     * ==================【说明给后端看的】==================
     * welcome 页不需要后端接口。
     * 这里展示的数据来自 login.vue 登录成功后 navigateTo 传参：
     *
     * /pages/welcome/welcome?
     *   username=...
     *   &password=...
     *   &captcha=...
     *   &token=...
     *
     * ✅ 真实项目建议：
     * - 不要传 password/captcha
     * - 只传 username 或者直接从 token 解出用户信息
     * =====================================================
     */
    this.username = query.username || ""
    this.password = query.password || ""
    this.captcha = query.captcha || ""
    this.token = query.token || ""
  },

  methods: {
    backToLogin() {
      uni.redirectTo({ url: "/pages/login/login" })
    }
  }
}
</script>

<style>
.page { padding: 40rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 40rpx; box-shadow: 0 10rpx 30rpx rgba(0,0,0,0.06); }
.title { font-size: 44rpx; font-weight: 800; margin-bottom: 26rpx; }
.row { margin-top: 18rpx; font-size: 30rpx; display: flex; flex-wrap: wrap; }
.k { color: #666; }
.v { color: #111; font-weight: 700; word-break: break-all; }
.btn { margin-top: 40rpx; background: #f2f2f2; border-radius: 12rpx; }
</style>
