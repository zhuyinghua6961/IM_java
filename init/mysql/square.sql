-- 广场模块表定义（独立于朋友圈）
USE im_chat;

-- 广场帖子表
DROP TABLE IF EXISTS `square_post`;
CREATE TABLE `square_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '正文内容',
  `images` JSON DEFAULT NULL COMMENT '图片URL数组',
  `video` VARCHAR(500) DEFAULT NULL COMMENT '视频URL',
  `tags` JSON DEFAULT NULL COMMENT '标签数组',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已删除 1-正常 2-审核未通过',
  `audit_status` TINYINT DEFAULT 0 COMMENT '审核状态 0-未审核/跳过 1-通过 2-拒绝',
  `audit_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核不通过原因',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广场帖子表';

-- 广场点赞表
DROP TABLE IF EXISTS `square_like`;
CREATE TABLE `square_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广场点赞表';

-- 广场评论表
DROP TABLE IF EXISTS `square_comment`;
CREATE TABLE `square_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '被回复的评论ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已删除 1-正常 2-审核未通过',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_time` (`post_id`,`create_time`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广场评论表';

-- 广场通知表
DROP TABLE IF EXISTS `square_notification`;
CREATE TABLE `square_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `comment_id` BIGINT DEFAULT NULL COMMENT '评论ID',
  `actor_id` BIGINT NOT NULL COMMENT '触发动作的用户ID',
  `action_type` VARCHAR(20) NOT NULL COMMENT '动作类型 LIKE/COMMENT',
  `message` VARCHAR(255) NOT NULL COMMENT '通知文案',
  `extra` TEXT DEFAULT NULL COMMENT '扩展信息(JSON)',
  `read` TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广场通知表';
