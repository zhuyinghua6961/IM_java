package com.im.message.constant;

/**
 * Redis Key常量
 */
public class RedisKeyConstant {
    
    /**
     * 消息详情 Key前缀
     * 格式: msg:detail:{messageId}
     * 类型: String
     * 值: Message JSON
     * 过期时间: 30分钟
     */
    public static final String MESSAGE_DETAIL_PREFIX = "msg:detail:";
    
    /**
     * 会话消息列表 Key前缀
     * 格式: msg:conv:{conversationId}
     * 类型: List
     * 值: List<Message> JSON
     * 过期时间: 30分钟
     */
    public static final String CONVERSATION_MESSAGES_PREFIX = "msg:conv:";
    
    /**
     * 用户未读消息 Key前缀
     * 格式: msg:unread:{userId}
     * 类型: Set
     * 值: Set<messageId>
     * 过期时间: 永久
     */
    public static final String USER_UNREAD_MESSAGES_PREFIX = "msg:unread:";
    
    /**
     * 用户已删除消息 Key前缀
     * 格式: msg:deleted:{userId}
     * 类型: Set
     * 值: Set<messageId>
     * 过期时间: 30分钟
     */
    public static final String USER_DELETED_MESSAGES_PREFIX = "msg:deleted:";
    
    /**
     * 消息撤回标记 Key前缀
     * 格式: msg:cancel:{messageId}
     * 类型: String
     * 值: "1"
     * 过期时间: 10分钟
     */
    public static final String MESSAGE_CANCEL_PREFIX = "msg:cancel:";
    
    /**
     * 消息消费锁 Key前缀
     * 格式: msg:lock:{messageId}
     * 类型: String
     * 值: "1"
     * 过期时间: 10秒
     */
    public static final String MESSAGE_LOCK_PREFIX = "msg:lock:";
    
    public static final String PENDING_MESSAGE_ZSET_KEY = "msg:pending";
    
    /**
     * 持久化状态：待持久化
     */
    public static final String PERSIST_STATUS_PENDING = "PENDING";
    
    /**
     * 持久化状态：已持久化
     */
    public static final String PERSIST_STATUS_PERSISTED = "PERSISTED";
    
    /**
     * 生成消息详情Key
     */
    public static String getMessageDetailKey(Long messageId) {
        return MESSAGE_DETAIL_PREFIX + messageId;
    }
    
    /**
     * 生成会话消息列表Key
     */
    public static String getConversationMessagesKey(String conversationId) {
        return CONVERSATION_MESSAGES_PREFIX + conversationId;
    }
    
    /**
     * 生成用户未读消息Key
     */
    public static String getUserUnreadMessagesKey(Long userId) {
        return USER_UNREAD_MESSAGES_PREFIX + userId;
    }
    
    /**
     * 生成用户已删除消息Key
     */
    public static String getUserDeletedMessagesKey(Long userId) {
        return USER_DELETED_MESSAGES_PREFIX + userId;
    }
    
    /**
     * 生成消息撤回标记Key
     */
    public static String getMessageCancelKey(Long messageId) {
        return MESSAGE_CANCEL_PREFIX + messageId;
    }
    
    /**
     * 生成消息消费锁Key
     */
    public static String getMessageLockKey(Long messageId) {
        return MESSAGE_LOCK_PREFIX + messageId;
    }
}
