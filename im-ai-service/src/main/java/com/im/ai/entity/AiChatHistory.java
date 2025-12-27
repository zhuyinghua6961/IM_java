package com.im.ai.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI客服聊天记录实体
 */
@Data
public class AiChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String message;

    private String reply;

    private String category;

    private LocalDateTime createTime;
}
