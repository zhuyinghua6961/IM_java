-- 消息表添加 at_user_ids 字段
ALTER TABLE `message` ADD COLUMN `at_user_ids` VARCHAR(500) DEFAULT NULL COMMENT '被@的用户ID列表，逗号分隔，all表示@全体成员' AFTER `persist_status`;
