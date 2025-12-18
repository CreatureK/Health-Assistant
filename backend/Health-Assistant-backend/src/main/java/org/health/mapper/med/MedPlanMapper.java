package org.health.mapper.med;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.med.MedPlan;

import java.util.List;

/**
 * 用药计划Mapper接口
 */
@Mapper
public interface MedPlanMapper {

    /**
     * 根据ID查询计划
     *
     * @param id 计划ID
     * @return 计划信息
     */
    MedPlan selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询计划列表
     *
     * @param userId 用户ID
     * @param status 状态过滤（active-进行中，expired-已过期）
     * @param keyword 关键词搜索
     * @return 计划列表
     */
    List<MedPlan> selectByUserId(@Param("userId") Long userId,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword);

    /**
     * 插入计划
     *
     * @param plan 计划信息
     * @return 影响行数
     */
    int insert(MedPlan plan);

    /**
     * 更新计划
     *
     * @param plan 计划信息
     * @return 影响行数
     */
    int update(MedPlan plan);

    /**
     * 软删除计划
     *
     * @param id 计划ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新提醒开关
     *
     * @param id 计划ID
     * @param remindEnabled 提醒开关
     * @return 影响行数
     */
    int updateRemindEnabled(@Param("id") Long id, @Param("remindEnabled") Boolean remindEnabled);
}

