package org.health.entity.wechat;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订阅授权实体类
 */
@Data
public class SubscribeGrant {
    /**
     * ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否授权：0-未授权，1-已授权
     */
    private Boolean granted;

    /**
     * 模板ID列表（JSON数组）
     */
    private List<String> templateIds;

    /**
     * 授权详情（JSON对象，如：{"TEMPLATE_ID_1": "accept"}）
     */
    private Map<String, String> detail;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

