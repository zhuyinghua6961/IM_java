package com.im.message.service;

import com.im.message.dto.MessageDTO;

import java.util.List;
import java.util.Map;

public interface MessageService {
    
    /**
     * 发送消息
     * @param fromUserId 发送者ID
     * @param messageDTO 消息内容
     * @return 消息ID
     */
    Long sendMessage(Long fromUserId, MessageDTO messageDTO);
    
    /**
     * 获取历史消息
     * @param userId 当前用户ID
     * @param targetId 对方ID（用户ID或群组ID）
     * @param chatType 聊天类型
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表和总数
     */
    Map<String, Object> getHistoryMessages(Long userId, Long targetId, Integer chatType, Integer page, Integer size);
    
    /**
     * 撤回消息
     * @param messageId 消息ID
     * @param userId 当前用户ID
     */
    void recallMessage(Long messageId, Long userId);
    
    /**
     * 标记消息已读
     * @param messageIds 消息ID列表
     * @param userId 当前用户ID
     */
    void markMessagesAsRead(List<Long> messageIds, Long userId);
    
    /**
     * 删除单条消息（仅对当前用户生效）
     * @param messageId 消息ID
     * @param userId 当前用户ID
     */
    void deleteMessage(Long messageId, Long userId);
}
