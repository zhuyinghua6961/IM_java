-- 群公告表
CREATE TABLE IF NOT EXISTS `group_announcement` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    `group_id` BIGINT NOT NULL COMMENT '群ID',
    `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_publisher_id` (`publisher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群公告表';
