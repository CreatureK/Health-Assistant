package org.health.entity.med;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 计划时间点实体类
 */
@Data
public class PlanTimes {
    /**
     * ID
     */
    private Long id;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 时间点（HH:mm格式）
     */
    private LocalTime time;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

