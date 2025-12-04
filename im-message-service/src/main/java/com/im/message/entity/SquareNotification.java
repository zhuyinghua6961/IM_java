package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场通知实体
 * 对应表：square_notification
 */
@Data
public class SquareNotification {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 接收通知的用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 帖子ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    /** 评论ID，可为空 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;

    /** 触发动作的用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long actorId;

    /** 动作类型：LIKE / COMMENT */
    private String actionType;

    /** 通知文案 */
    private String message;

    /** 扩展信息(JSON)，存放原始通知数据 */
    private String extra;

    /** 是否已读：0-未读 1-已读 */
    private Integer read;

    /** 创建时间 */
    private LocalDateTime createTime;
}
