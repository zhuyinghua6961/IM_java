-- 为群成员表添加禁言截止时间字段，用于实现群成员禁言功能
ALTER TABLE `group_member`
    ADD COLUMN `mute_until` DATETIME NULL DEFAULT NULL COMMENT '禁言截止时间，NULL 表示未禁言' AFTER `muted`;
