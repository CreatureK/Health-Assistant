package org.health.service;

import org.health.common.ResultCode;
import org.health.entity.Article;
import org.health.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 健康文章服务
 */
@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 查询文章列表
     *
     * @param category 分类（可选）
     * @param keyword 关键词（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 文章列表
     */
    public ArticleListVO getArticleList(String category, String keyword, Integer page, Integer size) {
        // 默认值
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }

        int offset = (page - 1) * size;

        // 查询列表
        List<Article> articles = articleMapper.selectList(category, keyword, offset, size);
        int total = articleMapper.countList(category, keyword);

        ArticleListVO vo = new ArticleListVO();
        vo.setList(articles.stream().map(this::convertToVO).collect(Collectors.toList()));
        vo.setPage(page);
        vo.setSize(size);
        vo.setTotal(total);

        return vo;
    }

    /**
     * 获取文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO getArticleDetail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException(ResultCode.NOT_FOUND.getMsg());
        }

        // 增加浏览次数
        articleMapper.incrementViewCount(id);
        article.setViewCount(article.getViewCount() + 1);

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setCategory(article.getCategory());
        vo.setContent(article.getContent());
        vo.setCoverImage(article.getCoverImage());
        vo.setViewCount(article.getViewCount());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());

        return vo;
    }

    /**
     * 转换为列表VO（包含摘要）
     */
    private ArticleVO convertToVO(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setCategory(article.getCategory());
        vo.setCoverImage(article.getCoverImage());
        vo.setViewCount(article.getViewCount());
        vo.setCreatedAt(article.getCreatedAt());
        
        // 生成摘要：从content中截取前100个字符
        String content = article.getContent();
        if (content != null && content.length() > 100) {
            vo.setDesc(content.substring(0, 100) + "...");
        } else {
            vo.setDesc(content != null ? content : "");
        }
        
        return vo;
    }

    /**
     * 文章列表视图对象
     */
    public static class ArticleListVO {
        private List<ArticleVO> list;
        private Integer page;
        private Integer size;
        private Integer total;

        public List<ArticleVO> getList() {
            return list;
        }

        public void setList(List<ArticleVO> list) {
            this.list = list;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    /**
     * 文章视图对象（列表）
     */
    public static class ArticleVO {
        private Long id;
        private String title;
        private String category;
        private String desc;  // 摘要
        private String coverImage;
        private Integer viewCount;
        private java.time.LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getCoverImage() {
            return coverImage;
        }

        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }

        public Integer getViewCount() {
            return viewCount;
        }

        public void setViewCount(Integer viewCount) {
            this.viewCount = viewCount;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 文章详情视图对象
     */
    public static class ArticleDetailVO {
        private Long id;
        private String title;
        private String category;
        private String content;
        private String coverImage;
        private Integer viewCount;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getCoverImage() {
            return coverImage;
        }

        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }

        public Integer getViewCount() {
            return viewCount;
        }

        public void setViewCount(Integer viewCount) {
            this.viewCount = viewCount;
        }

        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public java.time.LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}

