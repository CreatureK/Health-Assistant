<template>
  <view class="container">
    <view class="searchBar">
      <input
        class="searchInput"
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索药品名称（如：阿司匹林）"
        @confirm="onSearch"
      />
      <view class="searchBtn" @click="onSearch">搜索</view>
    </view>

    <view v-if="list.length === 0 && !loading" class="empty">
      暂无数据
    </view>

    <view v-for="item in list" :key="item.id" class="card" @click="openDetail(item)">
      <view class="row">
        <view class="name">{{ item.name }}</view>
      </view>
      <view v-if="item.tags && item.tags.length" class="tags">
        <view v-for="(t, idx) in item.tags" :key="idx" class="tag">{{ t }}</view>
      </view>
    </view>

    <view v-if="loading" class="hint">加载中...</view>
    <view v-else-if="finished && list.length > 0" class="hint">没有更多了</view>
  </view>
</template>

<script>
import { request } from "@/common/request.js";
import { API } from "@/common/api.js";

export default {
  data() {
    return {
      keyword: "",
      list: [],
      page: 1,
      size: 20,
      total: 0,
      loading: false,
      finished: false
    };
  },
  onLoad() {
    this.fetchList(true);
  },
  onPullDownRefresh() {
    this.fetchList(true).finally(() => uni.stopPullDownRefresh());
  },
  onReachBottom() {
    if (this.loading || this.finished) return;
    this.fetchList(false);
  },
  methods: {
    onSearch() {
      this.fetchList(true);
    },
    openDetail(item) {
      uni.navigateTo({
        url: `/pages/med/drug-detail?id=${encodeURIComponent(item.id)}`
      });
    },
    async fetchList(reset) {
      if (this.loading) return;
      this.loading = true;

      try {
        const nextPage = reset ? 1 : this.page + 1;

        // request() 成功时直接 resolve body.data（已在 request.js 处理 code===200）
        const payload = await request({
          url: API.medDrugs,
          method: "GET",
          data: {
<<<<<<< HEAD
            keyword: this.keyword ||"",
			// undefined,
=======
            keyword: this.keyword || undefined,
>>>>>>> origin/master
            page: nextPage,
            size: this.size
          }
        });

        const nextList = Array.isArray(payload?.list) ? payload.list : [];
        const nextTotal = Number(payload?.total || 0);

        if (reset) {
          this.list = nextList;
          this.page = nextPage;
        } else {
          this.list = this.list.concat(nextList);
          this.page = nextPage;
        }

        this.total = nextTotal;
        this.finished = this.list.length >= this.total;
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.container { padding: 24rpx 24rpx 40rpx; }
.searchBar { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.searchInput {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 18rpx 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.06); font-size: 28rpx;
}
.searchBtn {
  padding: 18rpx 26rpx; background: #fff; border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.06); font-size: 28rpx;
}
.card {
  background: #fff; padding: 26rpx; border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,0.06); margin-bottom: 18rpx;
}
.row { display: flex; align-items: center; justify-content: space-between; }
.name { font-size: 32rpx; font-weight: 700; }
.tags { margin-top: 14rpx; display: flex; flex-wrap: wrap; gap: 10rpx; }
.tag { font-size: 24rpx; padding: 6rpx 12rpx; background: #f5f5f5; border-radius: 999rpx; color: #444; }
.empty { text-align: center; color: #888; padding: 80rpx 0; }
.hint { text-align: center; color: #888; padding: 24rpx 0; }
</style>
