package org.health.entity.med;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药计划实体类
 */
@Data
public class MedPlan {
    /**
     * 计划ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 药品名称（1-50字符）
     */
    private String name;

    /**
     * 剂量（如：1片/10毫升）
     */
    private String dosage;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 重复类型：daily-每日，weekly-每周特定几天
     */
    private String repeatType;

    /**
     * 提醒开关：0-关闭，1-开启
     */
    private Boolean remindEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;
}

