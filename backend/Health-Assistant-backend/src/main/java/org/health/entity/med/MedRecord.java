package org.health.entity.med;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用药记录实体类（点位记录）
 */
@Data
public class MedRecord {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 计划名称（冗余字段，便于查询）
     */
    private String planName;

    /**
     * 剂量（冗余字段，便于查询）
     */
    private String dosage;

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 时间点（HH:mm格式）
     */
    private LocalTime time;

    /**
     * 状态：todo-待打卡，taken-已服用，missed-未服
     */
    private String status;

    /**
     * 实际操作时间
     */
    private LocalDateTime actionAt;

    /**
     * 备注
     */
    private String note;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

