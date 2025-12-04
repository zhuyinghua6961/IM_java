package com.im.square.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SquareFollow {
    private Long id;
    private Long followerId;
    private Long followeeId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
