-- 消息删除记录表
-- 记录用户删除了哪些消息（单向删除）

CREATE TABLE IF NOT EXISTS `message_delete` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '删除消息的用户ID',
  `message_id` BIGINT NOT NULL COMMENT '被删除的消息ID',
  `delete_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
  UNIQUE KEY `uk_user_message` (`user_id`, `message_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息删除记录表';
