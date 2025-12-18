<template>
  <view class="container">
    <view class="item" v-for="item in list" :key="item.id">
      <view class="title">{{ item.title }}</view>
      <view class="desc">{{ item.desc }}</view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      list: [
        {
          id: 1,
          title: "SDG3：确保健康的生活方式",
          desc: "促进所有年龄段人群的福祉。"
        },
        {
          id: 2,
          title: "合理用药的重要性",
          desc: "按时、按量、按医嘱服药是健康管理关键。"
        },
        {
          id: 3,
          title: "心理健康同样重要",
          desc: "关注情绪变化，必要时寻求专业帮助。"
        }
      ]
    };
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
</style>
