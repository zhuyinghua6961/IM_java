-- AI客服聊天记录表
CREATE TABLE `ai_chat_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `message` TEXT NOT NULL COMMENT '用户消息',
  `reply` TEXT NOT NULL COMMENT 'AI回复',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '问题分类',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服聊天记录表';
