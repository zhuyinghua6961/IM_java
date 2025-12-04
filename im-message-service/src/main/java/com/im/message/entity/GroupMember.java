package com.im.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群成员实体（用于消息服务查询）
 */
@Data
public class GroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;  // 1-群主 2-管理员 3-普通成员
    private Integer status;  // 1-正常 0-已退出
    private LocalDateTime joinTime;
    private LocalDateTime updateTime;
    private LocalDateTime muteUntil; // 禁言截止时间，null 表示未禁言
}
