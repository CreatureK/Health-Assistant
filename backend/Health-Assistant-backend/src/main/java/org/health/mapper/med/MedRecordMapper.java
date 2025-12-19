package org.health.mapper.med;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.health.entity.med.MedRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药记录Mapper接口
 */
@Mapper
public interface MedRecordMapper {

    /**
     * 根据ID查询记录
     *
     * @param id 记录ID
     * @return 记录信息
     */
    MedRecord selectById(@Param("id") Long id);

    /**
     * 根据计划ID、日期、时间查询记录（用于唯一性检查）
     *
     * @param planId 计划ID
     * @param date 日期
     * @param time 时间
     * @return 记录信息
     */
    MedRecord selectByPlanDateTime(@Param("planId") Long planId,
                                    @Param("date") LocalDate date,
                                    @Param("time") String time);

    /**
     * 根据日期查询记录列表
     *
     * @param userId 用户ID
     * @param date 日期
     * @return 记录列表
     */
    List<MedRecord> selectByDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询记录列表（支持多条件过滤）
     *
     * @param userId 用户ID
     * @param planId 计划ID（可选）
     * @param status 状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 记录列表
     */
    List<MedRecord> selectList(@Param("userId") Long userId,
                                @Param("planId") Long planId,
                                @Param("status") String status,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);

    /**
     * 统计记录总数
     *
     * @param userId 用户ID
     * @param planId 计划ID（可选）
     * @param status 状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 总数
     */
    int countList(@Param("userId") Long userId,
                   @Param("planId") Long planId,
                   @Param("status") String status,
                   @Param("startDate") LocalDate startDate,
                   @Param("endDate") LocalDate endDate);

    /**
     * 插入记录
     *
     * @param record 记录信息
     * @return 影响行数
     */
    int insert(MedRecord record);

    /**
     * 批量插入记录
     *
     * @param records 记录列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<MedRecord> records);

    /**
     * 更新记录状态
     *
     * @param id 记录ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新记录（补记/更正）
     *
     * @param record 记录信息
     * @return 影响行数
     */
    int update(MedRecord record);

    /**
     * 根据计划ID和日期范围删除记录
     *
     * @param planId 计划ID
     * @param startDate 开始日期
     * @return 影响行数
     */
    int deleteByPlanIdAndDate(@Param("planId") Long planId, @Param("startDate") LocalDate startDate);

    /**
     * 删除计划未来的todo记录
     * 只删除状态为'todo'且日期大于指定日期的记录
     *
     * @param planId 计划ID
     * @param startDate 开始日期（不包含，只删除大于此日期的记录）
     * @return 影响行数
     */
    int deleteFutureTodoRecordsByPlanId(@Param("planId") Long planId, @Param("startDate") LocalDate startDate);
}

