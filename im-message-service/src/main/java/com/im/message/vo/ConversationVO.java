package com.im.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long conversationId;
    private Long targetId;
    private Integer chatType;
    private String targetName;
    private String targetAvatar;
    private String lastMessage;
    private LocalDateTime lastMsgTime;
    private Integer unreadCount;
    private Boolean top;
    private Boolean hidden;
}
