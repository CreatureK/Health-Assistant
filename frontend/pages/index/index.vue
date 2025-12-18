<template>
	<view class="content">
		<image class="logo" src="/static/logo.png"></image>
		<view class="text-area">
			<text class="title">{{title}}</text>
		</view>
	</view>
</template>
<button @click="goPlanList">用药计划</button>
<button @click="goToday">今日用药</button>

<script>
export default {
  methods:{
    goPlanList(){
      uni.navigateTo({ url:'/pages/med/plan-list/plan-list' })
    },
    goToday(){
      uni.navigateTo({ url:'/pages/med/today-checkin/today-checkin' })
    }
  }
}
</script>
<style>
	.content {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.logo {
		height: 200rpx;
		width: 200rpx;
		margin-top: 200rpx;
		margin-left: auto;
		margin-right: auto;
		margin-bottom: 50rpx;
	}

	.text-area {
		display: flex;
		justify-content: center;
	}

	.title {
		font-size: 36rpx;
		color: #8f8f94;
	}
</style>
