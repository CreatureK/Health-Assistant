<template>
  <view class="container">
    <textarea v-model="content" placeholder="记录今天的健康状况…" />
    <button @click="save">保存记录</button>

    <view class="history">
      <view v-for="(item, i) in list" :key="i" class="item">
        {{ item }}
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      content: "",
      list: []
    };
  },
  methods: {
    save() {
      if (!this.content) return;
      this.list.unshift(this.content);
      this.content = "";
      uni.showToast({ title: "已保存", icon: "success" });
    }
  }
};
</script>

<style scoped>
.container {
  padding: 30rpx;
}
textarea {
  width: 100%;
  height: 200rpx;
  border: 1px solid #ddd;
  padding: 20rpx;
}
button {
  margin: 20rpx 0;
}
.item {
  background: #f8f8f8;
  padding: 16rpx;
  margin-bottom: 10rpx;
}
</style>
