package com.im.ai.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天消息实体
 */
@Data
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private Long id;

    private Long conversationId;

    private Long userId;

    private String role;

    private String content;

    private LocalDateTime createTime;
}
