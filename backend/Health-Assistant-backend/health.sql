-- ============================================
-- 健康助手数据库设计
-- 数据库名: health_assistant
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ============================================

CREATE DATABASE IF NOT EXISTS `health_assistant` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `health_assistant`;

-- ============================================
-- 第一阶段：核心功能表
-- ============================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `role` ENUM('elder', 'family') NOT NULL DEFAULT 'elder' COMMENT '角色：elder-老人，family-家属',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信openid',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 用药计划表
CREATE TABLE IF NOT EXISTS `med_plan` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '计划ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '药品名称（1-50字符）',
    `dosage` VARCHAR(50) NOT NULL COMMENT '剂量（如：1片/10毫升）',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `repeat_type` ENUM('daily', 'weekly') NOT NULL DEFAULT 'daily' COMMENT '重复类型：daily-每日，weekly-每周特定几天',
    `remind_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '提醒开关：0-关闭，1-开启',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_dates` (`start_date`, `end_date`),
    KEY `idx_deleted_at` (`deleted_at`),
    CONSTRAINT `fk_med_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- 3. 计划时间点表（一个计划可以有多个时间点）
CREATE TABLE IF NOT EXISTS `plan_times` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `plan_id` BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
    `time` TIME NOT NULL COMMENT '时间点（HH:mm格式）',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`),
    CONSTRAINT `fk_plan_times_plan` FOREIGN KEY (`plan_id`) REFERENCES `med_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划时间点表';

-- 4. 计划重复天数表（weekly类型需要）
CREATE TABLE IF NOT EXISTS `plan_repeat_days` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `plan_id` BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
    `day_of_week` TINYINT UNSIGNED NOT NULL COMMENT '星期几（0=周日，1=周一，...，6=周六）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_day` (`plan_id`, `day_of_week`),
    CONSTRAINT `fk_plan_repeat_days_plan` FOREIGN KEY (`plan_id`) REFERENCES `med_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划重复天数表';

-- 5. 用药记录表（点位记录，同一个plan+date+time只能有一条记录）
CREATE TABLE IF NOT EXISTS `med_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `plan_id` BIGINT UNSIGNED NOT NULL COMMENT '计划ID',
    `date` DATE NOT NULL COMMENT '日期',
    `time` TIME NOT NULL COMMENT '时间点（HH:mm格式）',
    `status` ENUM('todo', 'taken', 'missed') NOT NULL DEFAULT 'todo' COMMENT '状态：todo-待打卡，taken-已服用，missed-未服',
    `action_at` DATETIME DEFAULT NULL COMMENT '实际操作时间',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_date_time` (`plan_id`, `date`, `time`),
    KEY `idx_date` (`date`),
    KEY `idx_status` (`status`),
    KEY `idx_plan_id` (`plan_id`),
    CONSTRAINT `fk_med_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `med_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药记录表（点位记录）';

-- 6. 订阅授权表
CREATE TABLE IF NOT EXISTS `subscribe_grant` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `granted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否授权：0-未授权，1-已授权',
    `template_ids` JSON DEFAULT NULL COMMENT '模板ID列表（JSON数组）',
    `detail` JSON DEFAULT NULL COMMENT '授权详情（JSON对象，如：{"TEMPLATE_ID_1": "accept"}）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_subscribe_grant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅授权表';

-- 7. 药品库表
CREATE TABLE IF NOT EXISTS `drug_catalog` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '药品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '药品名称',
    `common_names` JSON DEFAULT NULL COMMENT '通用名称列表（JSON数组，如：["拜阿司匹林"]）',
    `tags` JSON DEFAULT NULL COMMENT '标签列表（JSON数组，如：["抗血小板"]）',
    `intro` TEXT DEFAULT NULL COMMENT '通俗说明',
    `usage` TEXT DEFAULT NULL COMMENT '一般如何使用（非处方建议，仅科普）',
    `warnings` TEXT DEFAULT NULL COMMENT '注意事项',
    `disclaimer` VARCHAR(500) DEFAULT NULL COMMENT '免责声明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    FULLTEXT KEY `ft_name_intro` (`name`, `intro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品库表';

-- ============================================
-- 第二阶段：其他功能表
-- ============================================

-- 8. 健康日记表
CREATE TABLE IF NOT EXISTS `health_diary` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日记ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `date` DATE NOT NULL COMMENT '日期',
    `sleep_quality` ENUM('good', 'normal', 'bad') DEFAULT NULL COMMENT '睡眠质量：good-好，normal-一般，bad-差',
    `spirit` ENUM('good', 'normal', 'bad') DEFAULT NULL COMMENT '精神状态：good-好，normal-一般，bad-差',
    `symptoms` JSON DEFAULT NULL COMMENT '症状（JSON对象，如：{"dizzy": true, "chestTight": false}）',
    `metrics` JSON DEFAULT NULL COMMENT '指标（JSON对象，如：{"bpSys": 130, "bpDia": 85, "glucose": 6.1, "hr": 72, "weight": 62.5}）',
    `note` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `date`),
    KEY `idx_date` (`date`),
    CONSTRAINT `fk_health_diary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康日记表';

-- 9. AI对话会话表
CREATE TABLE IF NOT EXISTS `ai_session` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_ai_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- 10. AI对话消息表
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
    `role` ENUM('user', 'assistant') NOT NULL COMMENT '角色：user-用户，assistant-助手',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `input_type` ENUM('text', 'voice') NOT NULL DEFAULT 'text' COMMENT '输入类型：text-文本，voice-语音',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_ai_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';

-- 11. 紧急联系人表
CREATE TABLE IF NOT EXISTS `emergency_contact` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) NOT NULL COMMENT '电话',
    `sort_order` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序顺序（限制1-2条）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_emergency_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急联系人表';

-- 12. 健康文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `content` LONGTEXT NOT NULL COMMENT '内容',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    `view_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_created_at` (`created_at`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康文章表';

-- 13. 家属绑定表
CREATE TABLE IF NOT EXISTS `family_binding` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
    `elder_id` BIGINT UNSIGNED NOT NULL COMMENT '老人用户ID',
    `family_id` BIGINT UNSIGNED NOT NULL COMMENT '家属用户ID',
    `code` VARCHAR(50) DEFAULT NULL COMMENT '绑定码',
    `code_expire_at` DATETIME DEFAULT NULL COMMENT '绑定码过期时间',
    `bound_at` DATETIME DEFAULT NULL COMMENT '绑定时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_elder_family` (`elder_id`, `family_id`),
    KEY `idx_code` (`code`),
    KEY `idx_elder_id` (`elder_id`),
    KEY `idx_family_id` (`family_id`),
    CONSTRAINT `fk_family_binding_elder` FOREIGN KEY (`elder_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_family_binding_family` FOREIGN KEY (`family_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家属绑定表';

-- ============================================
-- 索引优化说明
-- ============================================
-- 1. med_record表的uk_plan_date_time唯一索引确保同一个plan+date+time只有一条记录
-- 2. 所有外键都设置了ON DELETE CASCADE，删除主表记录时自动删除关联记录
-- 3. 使用软删除（deleted_at）的表支持数据恢复
-- 4. JSON字段用于存储灵活的数组和对象数据
-- 5. 全文索引（FULLTEXT）用于药品和文章的搜索功能
-- ============================================