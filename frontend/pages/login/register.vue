<template>
  <view class="page">
    <view class="card">
      <view class="header">
        <view class="title">注册</view>
        <view class="sub">创建账号后即可登录</view>
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
          <text class="label">确认密码</text>
          <input class="input" v-model.trim="form.password2" password placeholder="请再次输入密码" />
        </view>

        <view class="field">
          <view class="label-row">
            <text class="label">验证码</text>
            <text class="link" @click="fetchCaptcha">换一张</text>
          </view>

          <view class="captcha-row">
            <input class="input" v-model.trim="form.captchaCode" placeholder="请输入验证码" />
            <image class="captcha-img" :src="captchaImage" mode="aspectFit" @click="fetchCaptcha" />
          </view>
        </view>

        <button class="btn primary" :disabled="loading" @click="onRegister">
          {{ loading ? "注册中..." : "注册" }}
        </button>

        <button class="btn" @click="goLogin">返回登录</button>
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
        password2: "",
        captchaCode: ""
      }
    };
  },
  onLoad() {
    this.fetchCaptcha();
  },
  methods: {
    async fetchCaptcha() {
      try {
        const data = await request({ url: API.captcha, method: "GET" });
        this.captchaId = data.captchaId || "";
        this.captchaImage = data.imageBase64 || "";
      } catch (e) {
        this.captchaId = "";
        this.captchaImage = "";
      }
    },

    validate() {
      if (!this.form.username) return "请输入账号";
      if (!this.form.password) return "请输入密码";
      if (this.form.password.length < 6) return "密码至少6位";
      if (this.form.password !== this.form.password2) return "两次密码不一致";
      if (!this.captchaId) return "验证码未加载（点图片刷新）";
      if (!this.form.captchaCode) return "请输入验证码";
      return "";
    },

    async onRegister() {
      const msg = this.validate();
      if (msg) return uni.showToast({ title: msg, icon: "none" });

      this.loading = true;
      try {
        // ⚠️ 这里需要后端提供注册接口（见第4点），如果你们暂时没接口，就先注释掉 request，保留 UI
        await request({
          url: API.register,
          method: "POST",
          data: {
            username: this.form.username,
            password: this.form.password,
            captchaId: this.captchaId,
            captchaCode: this.form.captchaCode
          }
        });

        uni.showToast({ title: "注册成功，请登录", icon: "none" });
        uni.redirectTo({ url: "/pages/login/login" });
      } catch (e) {
        // request 内部已经 toast 过了，这里不重复
        this.fetchCaptcha();
      } finally {
        this.loading = false;
      }
    },

    goLogin() {
      uni.redirectTo({ url: "/pages/login/login" });
    }
  }
};
</script>

<style>
/* 同 login.vue：样式走全局 styles/common.css */
</style>
