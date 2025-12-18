package org.health.mapper.med;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.med.PlanTimes;

import java.util.List;

/**
 * 计划时间点Mapper接口
 */
@Mapper
public interface PlanTimesMapper {

    /**
     * 根据计划ID查询时间点列表
     *
     * @param planId 计划ID
     * @return 时间点列表
     */
    List<PlanTimes> selectByPlanId(@Param("planId") Long planId);

    /**
     * 批量插入时间点
     *
     * @param timesList 时间点列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<PlanTimes> timesList);

    /**
     * 根据计划ID删除时间点
     *
     * @param planId 计划ID
     * @return 影响行数
     */
    int deleteByPlanId(@Param("planId") Long planId);
}

