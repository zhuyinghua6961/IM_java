package com.im.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long messageId;
    private Long fromUserId;
    private String fromUserName;
    private String fromUserAvatar;
    private Long toId;
    private Integer chatType;
    private Integer msgType;
    private String content;
    private String url;
    private LocalDateTime sendTime;
    private Integer status;
    private LocalDateTime recallTime;
}
