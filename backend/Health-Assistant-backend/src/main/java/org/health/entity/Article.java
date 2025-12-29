package org.health.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 健康文章实体类
 */
@Data
public class Article {
    /**
     * 文章ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 分类（养生保健、慢病管理、运动与康复、心理健康与社交）
     */
    private String category;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

