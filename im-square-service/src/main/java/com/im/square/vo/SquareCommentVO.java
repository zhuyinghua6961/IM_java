package com.im.square.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场评论返回对象
 */
@Data
public class SquareCommentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String nickname;

    private String avatar;

    /** 被回复的评论ID，可为空 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String content;

    private LocalDateTime createTime;
}
