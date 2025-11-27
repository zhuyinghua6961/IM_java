package com.im.message.service;

/**
 * 消息通知服务接口
 */
public interface MessageNotificationService {
    
    /**
     * 通知消息已撤回
     * @param messageId 消息ID
     * @param fromUserId 发送者ID
     * @param targetId 目标ID（用户ID或群组ID）
     * @param chatType 聊天类型
     */
    void notifyMessageRecalled(Long messageId, Long fromUserId, Long targetId, Integer chatType);
    
    /**
     * 通知消息已读
     * @param messageIds 消息ID列表
     * @param userId 读取用户ID
     * @param targetId 目标ID
     * @param chatType 聊天类型
     */
    void notifyMessagesRead(java.util.List<Long> messageIds, Long userId, Long targetId, Integer chatType);
    
    /**
     * 通知新消息
     * @param message 消息对象
     */
    void notifyNewMessage(com.im.message.entity.Message message);
}
