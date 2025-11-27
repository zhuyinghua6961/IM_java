-- 为消息表添加撤回时间字段（如果不存在）
-- 检查字段是否存在，如果不存在则添加
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'message' 
     AND COLUMN_NAME = 'recall_time') = 0,
    'ALTER TABLE `message` ADD COLUMN `recall_time` DATETIME DEFAULT NULL COMMENT ''撤回时间'' AFTER `send_time`',
    'SELECT ''recall_time字段已存在'' as message'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
