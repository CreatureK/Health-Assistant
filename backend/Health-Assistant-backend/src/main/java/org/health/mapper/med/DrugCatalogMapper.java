package org.health.mapper.med;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.med.DrugCatalog;

import java.util.List;

/**
 * 药品库Mapper接口
 */
@Mapper
public interface DrugCatalogMapper {

    /**
     * 根据ID查询药品
     *
     * @param id 药品ID
     * @return 药品信息
     */
    DrugCatalog selectById(@Param("id") Long id);

    /**
     * 搜索药品列表
     *
     * @param keyword 关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 药品列表
     */
    List<DrugCatalog> searchDrugs(@Param("keyword") String keyword,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    /**
     * 统计药品总数
     *
     * @param keyword 关键词
     * @return 总数
     */
    int countDrugs(@Param("keyword") String keyword);
}

