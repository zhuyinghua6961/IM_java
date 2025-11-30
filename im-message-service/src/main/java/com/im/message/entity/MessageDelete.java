package com.im.message.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息删除记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDelete {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 删除消息的用户ID
     */
    private Long userId;
    
    /**
     * 被删除的消息ID
     */
    private Long messageId;
    
    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;
}
