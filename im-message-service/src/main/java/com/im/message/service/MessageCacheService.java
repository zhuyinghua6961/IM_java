package com.im.message.service;

import com.im.message.entity.Message;

import java.util.List;

/**
 * 消息缓存服务接口
 * 负责Redis缓存和Kafka异步持久化
 */
public interface MessageCacheService {
    
    /**
     * 缓存消息并发送到Kafka
     * @param message 消息对象
     */
    void cacheAndSendToKafka(Message message);
    
    /**
     * 从缓存获取会话消息列表
     * @param conversationId 会话ID
     * @param limit 数量限制
     * @return 消息列表
     */
    List<Message> getConversationMessagesFromCache(String conversationId, int limit);
    
    /**
     * 从缓存获取单条消息
     * @param messageId 消息ID
     * @return 消息对象
     */
    Message getMessageFromCache(Long messageId);
    
    /**
     * 标记消息为撤回状态（Redis + Kafka）
     * @param messageId 消息ID
     * @param persistStatus 持久化状态
     */
    void markMessageAsRecalled(Long messageId, String persistStatus);
    
    /**
     * 检查消息是否已删除
     * @param userId 用户ID
     * @param messageId 消息ID
     * @return true-已删除
     */
    boolean isMessageDeleted(Long userId, Long messageId);
    
    /**
     * 标记消息已删除
     * @param userId 用户ID
     * @param messageId 消息ID
     */
    void markMessageAsDeleted(Long userId, Long messageId);
    
    /**
     * 生成会话ID
     * @param userId1 用户1
     * @param userId2 用户2
     * @return 会话ID
     */
    String generateConversationId(Long userId1, Long userId2);
    
    /**
     * 检查会话缓存是否已加载
     * @param conversationId 会话ID
     * @return true-已加载
     */
    boolean isConversationCacheLoaded(String conversationId);
    
    /**
     * 加载历史消息到Redis缓存
     * @param conversationId 会话ID
     * @param messages 消息列表
     */
    void loadHistoryMessagesToCache(String conversationId, List<Message> messages);
    
    /**
     * 标记会话缓存已加载
     * @param conversationId 会话ID
     */
    void markConversationCacheLoaded(String conversationId);
}
