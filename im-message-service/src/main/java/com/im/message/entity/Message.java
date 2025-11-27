package com.im.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
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
}
