package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long fromUserId;
    private Long toId;
    private Integer chatType;
    private Integer msgType;
    private String content;
    private String url;
    private Integer status;
    private LocalDateTime sendTime;
    private LocalDateTime recallTime;
    private String persistStatus; // 持久化状态：PENDING-待持久化, PERSISTED-已持久化
    private String atUserIds; // 被@的用户ID列表，逗号分隔，"all"表示@全体成员
}
