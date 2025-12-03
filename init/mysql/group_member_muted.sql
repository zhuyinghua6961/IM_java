-- 群成员表添加免打扰字段
ALTER TABLE `group_member` ADD COLUMN `muted` TINYINT(1) DEFAULT 0 COMMENT '免打扰：0-正常，1-免打扰' AFTER `status`;
