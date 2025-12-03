-- 好友表添加免打扰字段
ALTER TABLE `friend` ADD COLUMN `muted` TINYINT(1) DEFAULT 0 COMMENT '免打扰：0-正常，1-免打扰' AFTER `status`;
