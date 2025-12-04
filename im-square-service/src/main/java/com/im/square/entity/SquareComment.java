package com.im.square.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场评论实体，对应表 square_comment
 */
@Data
public class SquareComment {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 被回复的评论ID，可为空 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String content;

    /** 状态 0-已删除 1-正常 2-审核未通过 */
    private Integer status;

    private LocalDateTime createTime;
}
