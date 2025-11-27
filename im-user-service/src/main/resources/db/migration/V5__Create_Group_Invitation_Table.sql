-- 群组邀请表
CREATE TABLE `group_invitation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `inviter_id` BIGINT NOT NULL COMMENT '邀请人ID',
  `invitee_id` BIGINT NOT NULL COMMENT '被邀请人ID',
  `inviter_role` TINYINT NOT NULL DEFAULT 0 COMMENT '邀请人角色：0=普通成员 1=管理员 2=群主',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=待管理员审批 1=待被邀请人同意 2=已同意 3=管理员拒绝 4=被邀请人拒绝 5=已过期',
  `admin_reviewer_id` BIGINT COMMENT '审批的管理员ID（预留）',
  `admin_review_time` DATETIME COMMENT '审批时间（预留）',
  `admin_review_note` VARCHAR(200) COMMENT '审批备注（预留）',
  `invitee_reply_time` DATETIME COMMENT '被邀请人回复时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_inviter_id` (`inviter_id`),
  KEY `idx_invitee_id` (`invitee_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组邀请表';
