package org.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.Article;

import java.util.List;

/**
 * 健康文章Mapper接口
 */
@Mapper
public interface ArticleMapper {

    /**
     * 根据ID查询文章
     *
     * @param id 文章ID
     * @return 文章信息
     */
    Article selectById(@Param("id") Long id);

    /**
     * 查询文章列表
     *
     * @param category 分类（可选）
     * @param keyword 关键词（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文章列表
     */
    List<Article> selectList(@Param("category") String category,
                              @Param("keyword") String keyword,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    /**
     * 统计文章总数
     *
     * @param category 分类（可选）
     * @param keyword 关键词（可选）
     * @return 总数
     */
    int countList(@Param("category") String category,
                  @Param("keyword") String keyword);

    /**
     * 增加浏览次数
     *
     * @param id 文章ID
     */
    void incrementViewCount(@Param("id") Long id);
}

