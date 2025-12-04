package com.im.square.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class SquareProfileVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String nickname;

    private String avatar;

    private Long fansCount;

    private Long followCount;

    private Long postCount;

    private Long likeCount;

    private Boolean followed;

    private Boolean self;
}
