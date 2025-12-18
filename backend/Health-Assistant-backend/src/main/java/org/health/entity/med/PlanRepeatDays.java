package org.health.entity.med;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 计划重复天数实体类
 */
@Data
public class PlanRepeatDays {
    /**
     * ID
     */
    private Long id;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 星期几（0=周日，1=周一，...，6=周六）
     */
    private Integer dayOfWeek;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

