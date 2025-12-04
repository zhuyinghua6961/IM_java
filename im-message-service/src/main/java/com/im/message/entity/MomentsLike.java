package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态点赞实体，对应表：moments_like
 */
@Data
public class MomentsLike {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long momentsId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private LocalDateTime createTime;
}
