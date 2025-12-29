<template>
  <view class="page">
    <view class="card">
      <view class="header">
        <view class="title">登录</view>
        <view class="sub">欢迎回来，请使用账号（邮箱）登录</view>
      </view>

      <view class="form">
        <view class="field">
          <text class="label">账号</text>
          <input class="input" v-model.trim="form.username" placeholder="请输入账号（邮箱）" />
        </view>

        <view class="field">
          <text class="label">密码</text>
          <input class="input" v-model.trim="form.password" password placeholder="请输入密码" />
        </view>

        <view class="field">
          <view class="label-row">
            <text class="label">验证码</text>
            <text class="link" @click="!loading && fetchCaptcha()">换一张</text>
          </view>

          <view class="captcha-row">
            <input class="input" v-model.trim="form.captchaCode" placeholder="请输入验证码" />
            <image
              class="captcha-img"
              :src="captchaImage"
              mode="aspectFit"
              @click="!loading && fetchCaptcha()"
            />
          </view>
        </view>

        <button class="btn primary" :disabled="loading" @click="onLogin">
          {{ loading ? "登录中..." : "登录" }}
        </button>

        <button class="btn" :disabled="loading" @click="goRegister">
          注册
        </button>

        <view class="tips" v-if="!captchaImage">
          <text>验证码加载失败：检查后端是否启动、BASE_URL、是否勾选“不校验合法域名”。</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request, API } from "@/common/request";

export default {
  data() {
    return {
      loading: false,
      captchaId: "",
      captchaImage: "",
      form: {
        username: "",
        password: "",
        captchaCode: ""
      }
    };
  },
  onLoad() {
    this.fetchCaptcha();
  },
  methods: {
    async fetchCaptcha() {
      if (this.loading) return;

      try {
        // ✅ request.js 已解包：这里直接拿到 payload
        // payload: { captchaId, imageBase64, expireIn }
        const cap = await request({ url: API.captcha, method: "GET" });

        this.captchaId = cap?.captchaId || "";
        this.captchaImage = cap?.imageBase64 || ""; // 已带 data:image/png;base64,
        this.form.captchaCode = "";

        console.log("[captcha OK]", this.captchaId, (this.captchaImage || "").slice(0, 30));
      } catch (e) {
        this.captchaId = "";
        this.captchaImage = "";
        this.form.captchaCode = "";
        console.error("[captcha FAILED]", e);
      }
    },

    goRegister() {
      uni.navigateTo({ url: "/pages/login/register" });
    },

    validate() {
      if (!this.form.username) return "请输入账号";
      if (!this.form.password) return "请输入密码";
      if (!this.form.captchaCode) return "请输入验证码";
      if (!this.captchaId) return "验证码未加载（点图片刷新）";
      return "";
    },

    async onLogin() {
      const msg = this.validate();
      if (msg) return uni.showToast({ title: msg, icon: "none" });

      const submitCaptchaId = this.captchaId;
      this.loading = true;

      try {
        // ✅ request.js 已解包：这里直接拿到 payload
        // payload: { token, user }
        const payload = await request({
          url: API.login,
          method: "POST",
          data: {
            username: this.form.username,
            password: this.form.password,
            captchaId: submitCaptchaId,
            captchaCode: this.form.captchaCode
          }
        });

        const token = payload?.token;
        const user = payload?.user;

        if (!token) {
          uni.showToast({ title: "登录返回缺少token", icon: "none" });
          return;
        }

        uni.setStorageSync("token", token);
        uni.setStorageSync("user", user || null);

        uni.showToast({ title: "登录成功", icon: "success" });
        uni.reLaunch({ url: "/pages/hub/entry" });
      } catch (e) {
        // ✅ request.js 会把业务失败 reject 出来（包含 msg）
        const msg = e?.msg || e?.message || "登录失败";
        uni.showToast({ title: msg, icon: "none" });

        // 登录失败刷新验证码
        await this.fetchCaptcha();
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style>
/*
  本页视觉样式统一放在全局样式：@/styles/common.css
  这里保持空，避免和全局样式打架。
*/
</style>
