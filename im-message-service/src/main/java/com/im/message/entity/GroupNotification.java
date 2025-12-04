package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群消息通知实体
 * 对应表：group_notification
 */
@Data
public class GroupNotification {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 接收通知的用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 群组ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /** 通知类型，例如：GROUP_MEMBER_MUTED */
    private String type;

    /** 通知文案 */
    private String message;

    /** 扩展信息，存放原始通知JSON */
    private String extra;

    /** 是否已读：0-未读，1-已读 */
    private Integer read;

    /** 创建时间 */
    private LocalDateTime createTime;
}
