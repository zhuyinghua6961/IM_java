package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态评论实体，对应表：moments_comment
 */
@Data
public class MomentsComment {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long momentsId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 回复的评论ID，可为空 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToId;

    private String content;

    /** 状态 0-已删除 1-正常 */
    private Integer status;

    private LocalDateTime createTime;
}
