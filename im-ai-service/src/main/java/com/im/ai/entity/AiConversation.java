package com.im.ai.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI会话实体
 */
@Data
public class AiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private Integer messageCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
