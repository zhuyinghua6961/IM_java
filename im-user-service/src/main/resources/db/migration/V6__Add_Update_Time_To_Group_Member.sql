-- 为 group_member 表添加 update_time 字段
-- 创建时间: 2025-11-20
-- 版本: V6

-- 添加 update_time 字段
ALTER TABLE `group_member` 
ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `status`;

-- 为现有数据设置 update_time 为 join_time
UPDATE `group_member` SET `update_time` = `join_time` WHERE `update_time` IS NULL;
