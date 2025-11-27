package com.im.message.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知DTO
 */
@Data
@Builder
public class MessageNotificationDTO {
    
    /**
     * 通知类型
     */
    private String type;
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 消息ID列表（批量操作时使用）
     */
    private List<Long> messageIds;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 发送者ID
     */
    private Long fromUserId;
    
    /**
     * 目标ID
     */
    private Long targetId;
    
    /**
     * 聊天类型
     */
    private Integer chatType;
    
    /**
     * 消息内容（新消息时使用）
     */
    private String content;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 撤回时间
     */
    private LocalDateTime recallTime;
    
    // 通知类型常量
    public static final String TYPE_NEW_MESSAGE = "NEW_MESSAGE";
    public static final String TYPE_MESSAGE_RECALLED = "MESSAGE_RECALLED";
    public static final String TYPE_MESSAGES_READ = "MESSAGES_READ";
    public static final String TYPE_CONVERSATION_UPDATED = "CONVERSATION_UPDATED";
}
