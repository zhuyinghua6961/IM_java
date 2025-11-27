package com.im.message.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Conversation extends BaseEntity {
    private Long userId;
    private Long targetId;
    private Integer chatType;
    private Long lastMsgId;
    private Integer unreadCount;
    private Integer top;
    private Integer hidden;
}
