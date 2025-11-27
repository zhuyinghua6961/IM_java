-- IM Chat System Database Initialization Script
-- 创建时间: 2024-01-01
-- 数据库版本: 1.0.0

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
# 选择im_chat数据库
USE im_chat;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 好友关系表
-- ----------------------------
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `friend_id` BIGINT NOT NULL COMMENT '好友ID',
  `remark` VARCHAR(50) DEFAULT NULL COMMENT '备注名',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已删除 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ----------------------------
-- 好友申请表
-- ----------------------------
DROP TABLE IF EXISTS `friend_request`;
CREATE TABLE `friend_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `from_user_id` BIGINT NOT NULL COMMENT '申请人ID',
  `to_user_id` BIGINT NOT NULL COMMENT '接收人ID',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '申请消息',
  `status` TINYINT DEFAULT 0 COMMENT '状态 0-待处理 1-已同意 2-已拒绝',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_to_user` (`to_user_id`, `status`),
  KEY `idx_from_user` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- ----------------------------
-- 群组表
-- ----------------------------
DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `group_name` VARCHAR(50) NOT NULL COMMENT '群名称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '群头像',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `notice` VARCHAR(500) DEFAULT NULL COMMENT '群公告',
  `max_members` INT DEFAULT 500 COMMENT '最大成员数',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已解散 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ----------------------------
-- 群成员表
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` TINYINT DEFAULT 0 COMMENT '角色 0-普通成员 1-管理员 2-群主',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '群昵称',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已退出 1-正常',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ----------------------------
-- 消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id` BIGINT NOT NULL COMMENT '发送者ID',
  `to_id` BIGINT NOT NULL COMMENT '接收者ID(用户ID或群组ID)',
  `chat_type` TINYINT NOT NULL COMMENT '聊天类型 1-单聊 2-群聊',
  `msg_type` TINYINT NOT NULL COMMENT '消息类型 1-文本 2-图片 3-视频 4-文件 5-语音',
  `content` TEXT COMMENT '消息内容',
  `url` VARCHAR(500) DEFAULT NULL COMMENT '媒体文件URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已撤回 1-正常',
  `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `recall_time` DATETIME DEFAULT NULL COMMENT '撤回时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat` (`to_id`, `chat_type`, `send_time`),
  KEY `idx_from_user` (`from_user_id`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- 消息已读表
-- ----------------------------
DROP TABLE IF EXISTS `message_read`;
CREATE TABLE `message_read` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` BIGINT NOT NULL COMMENT '消息ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_user` (`message_id`, `user_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读表';

-- ----------------------------
-- 会话表
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '对方ID(用户ID或群组ID)',
  `chat_type` TINYINT NOT NULL COMMENT '聊天类型 1-单聊 2-群聊',
  `last_msg_id` BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
  `unread_count` INT DEFAULT 0 COMMENT '未读数',
  `top` INT NOT NULL DEFAULT 0 COMMENT '是否置顶 0-否 1-是',
  `hidden` INT NOT NULL DEFAULT 0 COMMENT '是否隐藏 0-否 1-是',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `chat_type`),
  KEY `idx_user_update` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ----------------------------
-- 朋友圈动态表
-- ----------------------------
DROP TABLE IF EXISTS `moments`;
CREATE TABLE `moments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content` TEXT COMMENT '文字内容',
  `images` JSON DEFAULT NULL COMMENT '图片URL数组',
  `video` VARCHAR(500) DEFAULT NULL COMMENT '视频URL',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '位置',
  `visible_type` TINYINT DEFAULT 0 COMMENT '可见范围 0-公开 1-私密 2-部分可见',
  `visible_users` JSON DEFAULT NULL COMMENT '可见用户ID数组',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `comment_count` INT DEFAULT 0 COMMENT '评论数',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已删除 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈动态表';

-- ----------------------------
-- 朋友圈点赞表
-- ----------------------------
DROP TABLE IF EXISTS `moments_like`;
CREATE TABLE `moments_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moments_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moments_user` (`moments_id`, `user_id`),
  KEY `idx_moments_id` (`moments_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈点赞表';

-- ----------------------------
-- 朋友圈评论表
-- ----------------------------
DROP TABLE IF EXISTS `moments_comment`;
CREATE TABLE `moments_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `moments_id` BIGINT NOT NULL COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '评论人ID',
  `reply_to_id` BIGINT DEFAULT NULL COMMENT '回复的评论ID',
  `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-已删除 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_moments_id` (`moments_id`, `create_time`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈评论表';

-- ----------------------------
-- 插入测试数据
-- ----------------------------
-- 插入测试用户 (密码都是: 123456, BCrypt加密)
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `gender`, `phone`, `email`, `signature`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'https://via.placeholder.com/150', 1, '13800138000', 'admin@im.com', '这是管理员账号'),
(2, 'user001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'https://via.placeholder.com/150', 1, '13800138001', 'user001@im.com', '你好，我是张三'),
(3, 'user002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'https://via.placeholder.com/150', 2, '13800138002', 'user002@im.com', '你好，我是李四'),
(4, 'user003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'https://via.placeholder.com/150', 1, '13800138003', 'user003@im.com', '你好，我是王五');

-- 插入好友关系
INSERT INTO `friend` (`user_id`, `friend_id`, `remark`) VALUES
(1, 2, '张三'),
(1, 3, '李四'),
(2, 1, '管理员'),
(2, 3, '李四'),
(3, 1, '管理员'),
(3, 2, '张三');

-- 插入测试群组
INSERT INTO `group` (`id`, `group_name`, `avatar`, `owner_id`, `notice`, `max_members`) VALUES
(1, '技术交流群', 'https://via.placeholder.com/150', 1, '欢迎大家加入技术交流群！', 500),
(2, '产品讨论组', 'https://via.placeholder.com/150', 1, '产品需求讨论', 200);

-- 插入群成员
INSERT INTO `group_member` (`group_id`, `user_id`, `role`, `nickname`) VALUES
(1, 1, 2, '群主'),
(1, 2, 0, '张三'),
(1, 3, 1, '李四'),
(2, 1, 2, '群主'),
(2, 2, 0, '张三');

SET FOREIGN_KEY_CHECKS = 1;
