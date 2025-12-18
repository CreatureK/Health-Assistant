package org.health.entity.med;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品库实体类
 */
@Data
public class DrugCatalog {
    /**
     * 药品ID
     */
    private Long id;

    /**
     * 药品名称
     */
    private String name;

    /**
     * 通用名称列表（JSON数组）
     */
    private List<String> commonNames;

    /**
     * 标签列表（JSON数组）
     */
    private List<String> tags;

    /**
     * 通俗说明
     */
    private String intro;

    /**
     * 一般如何使用（非处方建议，仅科普）
     */
    private String usage;

    /**
     * 注意事项
     */
    private String warnings;

    /**
     * 免责声明
     */
    private String disclaimer;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

