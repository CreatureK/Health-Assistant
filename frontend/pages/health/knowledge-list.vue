<template>
  <view class="page">
    <view class="searchbar">
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索文章关键字…"
        confirm-type="search"
        @confirm="onSearch"
      />
      <button class="btn ghost" size="mini" @click="onSearch">搜索</button>
      <button class="btn ghost" size="mini" @click="reset">重置</button>
    </view>

    <view class="filters">
      <picker :range="categories" :value="catIndex" @change="onPickCategory">
        <view class="pill">
          <text>分类：</text>
          <text class="v">{{ categories[catIndex] }}</text>
        </view>
      </picker>
    </view>

    <scroll-view class="list" scroll-y @scrolltolower="loadMore">
      <view
        class="item"
        v-for="item in list"
        :key="item.id"
        @click="openDetail(item)"
      >
        <view class="title">{{ item.title || item.name }}</view>
        <view class="desc">
          {{ item.summary || item.desc || item.brief || "点击查看详情" }}
        </view>
        <view class="meta">
          <text v-if="item.category">{{ item.category }}</text>
          <text v-if="item.createdAt">{{ item.createdAt }}</text>
        </view>
      </view>

      <view class="state" v-if="loading">加载中...</view>
      <view class="state" v-else-if="list.length === 0">暂无数据</view>
      <view class="state" v-else-if="finished">没有更多了</view>
    </scroll-view>

    <!-- 详情弹窗 -->
    <view class="modal-mask" v-if="detailVisible" @click="detailVisible = false">
      <view class="modal" @click.stop>
        <view class="modal-title">{{ detail.title || detail.name }}</view>
        <scroll-view class="modal-body" scroll-y>
          <text class="modal-text">{{
            detail.content ||
            detail.body ||
            detail.text ||
            detail.desc ||
            "暂无内容"
          }}</text>
          <view class="modal-note" v-if="detail.disclaimer">
            {{ detail.disclaimer }}
          </view>
        </scroll-view>

        <view class="modal-actions">
          <button class="btn" size="mini" @click="detailVisible = false">
            关闭
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from "@/common/request.js";
import { API } from "@/common/api.js";

function buildQuery(params) {
  const clean = Object.entries(params || {}).filter(
    ([, v]) => v !== undefined && v !== null && v !== ""
  );
  if (clean.length === 0) return "";
  return (
    "?" +
    clean
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join("&")
  );
}

export default {
  data() {
    return {
      keyword: "",
      categories: ["全部"],
      catIndex: 0,

      page: 1,
      size: 10,
      total: 0,
      list: [],
      loading: false,
      finished: false,

      detailVisible: false,
      detail: {}
    };
  },

  onLoad() {
    this.fetchList(true);
  },

  methods: {
    async fetchList(reset = false) {
      if (this.loading) return;

      if (reset) {
        this.page = 1;
        this.list = [];
        this.finished = false;
        this.total = 0;
      }
      if (this.finished) return;

      this.loading = true;

      try {
        // 文档：GET /api/v1/articles?category&keyword&page&size :contentReference[oaicite:9]{index=9}
        const category =
          this.categories[this.catIndex] && this.categories[this.catIndex] !== "全部"
            ? this.categories[this.catIndex]
            : "";

        const data = await request({
          url:
            API.articles +
            buildQuery({
              category,
              keyword: this.keyword,
              page: this.page,
              size: this.size
            })
        });

        // 兼容：后端可能返回 {list,page,size,total} 或直接数组
        const list = Array.isArray(data) ? data : data?.list || [];
        const page = Array.isArray(data) ? this.page : Number(data?.page || this.page);
        const size = Array.isArray(data) ? this.size : Number(data?.size || this.size);
        const total = Array.isArray(data) ? list.length : Number(data?.total || 0);

        this.total = total;

        // 首次抽取分类（如果后端字段里带 category）
        if (reset && this.categories.length === 1) {
          const cats = Array.from(
            new Set(list.map((x) => x.category).filter(Boolean))
          );
          if (cats.length) this.categories = ["全部", ...cats];
        }

        this.list = this.list.concat(list);

        // 判定是否结束
        if (Array.isArray(data)) {
          // 如果后端直接给数组，就按“本次小于 size”判断
          if (list.length < this.size) this.finished = true;
        } else {
          const loaded = this.list.length;
          if (total && loaded >= total) this.finished = true;
          if (!total && list.length < size) this.finished = true;
        }

        this.page = page + 1;
      } catch (e) {
        // request.js 已 toast
      } finally {
        this.loading = false;
      }
    },

    onSearch() {
      this.fetchList(true);
    },

    reset() {
      this.keyword = "";
      this.catIndex = 0;
      this.fetchList(true);
    },

    onPickCategory(e) {
      const idx = Number(e?.detail?.value || 0);
      this.catIndex = idx;
      this.fetchList(true);
    },

    loadMore() {
      this.fetchList(false);
    },

    async openDetail(item) {
      try {
        const data = await request({
          url: API.articleDetail(item.id)
        });
        this.detail = data || item || {};
        this.detailVisible = true;
      } catch (e) {}
    }
  }
};
</script>

<style scoped>
.page {
  height: 100vh;
  background: #f7f7f7;
  display: flex;
  flex-direction: column;
}

.searchbar {
  display: flex;
  align-items: center;
  padding: 16rpx;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.search-input {
  flex: 1;
  background: #f7f7f7;
  border-radius: 12rpx;
  padding: 0 16rpx;
  height: 72rpx;
  line-height: 72rpx;
  margin-right: 12rpx;
}

.filters {
  padding: 12rpx 16rpx;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 10rpx 14rpx;
  background: #f7f7f7;
  border-radius: 999rpx;
  font-size: 26rpx;
}
.pill .v {
  margin-left: 6rpx;
  color: #2d6cdf;
}

.btn {
  background: #2d6cdf;
  color: #fff;
  border-radius: 12rpx;
  padding: 0 18rpx;
  height: 64rpx;
  line-height: 64rpx;
}
.btn.ghost {
  background: #fff;
  color: #2d6cdf;
  border: 1px solid #cfe0ff;
  margin-left: 10rpx;
}

.list {
  flex: 1;
  padding: 16rpx;
}

.item {
  background: #fff;
  border-radius: 16rpx;
  padding: 18rpx 18rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.04);
}

.title {
  font-weight: 700;
  font-size: 30rpx;
  color: #222;
}

.desc {
  color: #666;
  font-size: 26rpx;
  margin-top: 10rpx;
  line-height: 1.5;
}

.meta {
  margin-top: 12rpx;
  display: flex;
  justify-content: space-between;
  color: #999;
  font-size: 24rpx;
}

.state {
  text-align: center;
  color: #999;
  padding: 30rpx 0 60rpx;
}

/* modal */
.modal-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24rpx;
}

.modal {
  width: 100%;
  max-width: 680rpx;
  background: #fff;
  border-radius: 18rpx;
  overflow: hidden;
}

.modal-title {
  padding: 18rpx;
  font-size: 32rpx;
  font-weight: 700;
  border-bottom: 1px solid #eee;
}

.modal-body {
  max-height: 70vh;
  padding: 18rpx;
}

.modal-text {
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 28rpx;
  color: #222;
}

.modal-note {
  margin-top: 16rpx;
  padding: 12rpx;
  background: #fff7e6;
  border-radius: 12rpx;
  color: #a66b00;
  font-size: 24rpx;
}

.modal-actions {
  padding: 14rpx 18rpx;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
}
</style>
