-- 群消息通知表，用于持久化群相关系统通知（例如禁言变更、成员变更等）
CREATE TABLE IF NOT EXISTS `group_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `type` VARCHAR(64) NOT NULL COMMENT '通知类型，例如：GROUP_MEMBER_MUTED',
    `message` VARCHAR(500) NOT NULL COMMENT '通知文案',
    `extra` JSON DEFAULT NULL COMMENT '扩展信息（操作者、禁言时间等）',
    `read` TINYINT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_group_time` (`group_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息通知表';
