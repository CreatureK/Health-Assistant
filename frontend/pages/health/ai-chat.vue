<template>
  <view class="container">
    <view class="chat-box">
      <view v-for="(msg, i) in messages" :key="i" class="msg">
        {{ msg }}
      </view>
    </view>

    <view class="input-box">
      <input v-model="input" placeholder="请输入你的健康问题…" />
      <button @click="send">发送</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      input: "",
      messages: ["你好，我是你的 AI 健康助手 🤖"]
    };
  },
  methods: {
    send() {
      if (!this.input) return;
      this.messages.push("你：" + this.input);
      this.messages.push("AI：建议保持规律作息，如有不适请咨询医生。");
      this.input = "";
    }
  }
};
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.chat-box {
  flex: 1;
  padding: 20rpx;
}
.msg {
  margin-bottom: 16rpx;
}
.input-box {
  display: flex;
  padding: 20rpx;
}
input {
  flex: 1;
  border: 1px solid #ddd;
  padding: 10rpx;
}
button {
  margin-left: 10rpx;
}
</style>
