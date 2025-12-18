package org.health.mapper.med;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.med.PlanRepeatDays;

import java.util.List;

/**
 * 计划重复天数Mapper接口
 */
@Mapper
public interface PlanRepeatDaysMapper {

    /**
     * 根据计划ID查询重复天数列表
     *
     * @param planId 计划ID
     * @return 重复天数列表
     */
    List<PlanRepeatDays> selectByPlanId(@Param("planId") Long planId);

    /**
     * 批量插入重复天数
     *
     * @param daysList 重复天数列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<PlanRepeatDays> daysList);

    /**
     * 根据计划ID删除重复天数
     *
     * @param planId 计划ID
     * @return 影响行数
     */
    int deleteByPlanId(@Param("planId") Long planId);
}

