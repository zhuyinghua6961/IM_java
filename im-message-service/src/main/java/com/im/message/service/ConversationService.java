package com.im.message.service;

import com.im.message.vo.ConversationVO;

import java.util.List;

public interface ConversationService {
    
    /**
     * 获取会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ConversationVO> getConversationList(Long userId);
    
    /**
     * 清空未读数
     * @param userId 用户ID
     * @param targetId 对方ID
     * @param chatType 聊天类型
     */
    void clearUnreadCount(Long userId, Long targetId, Integer chatType);
    
    /**
     * 置顶会话
     * @param conversationId 会话ID
     * @param top 是否置顶
     */
    void topConversation(Long conversationId, Boolean top);
    
    /**
     * 更新或创建会话
     * @param userId 用户ID
     * @param targetId 对方ID
     * @param chatType 聊天类型
     * @param lastMsgId 最后一条消息ID
     * @param incrementUnread 是否增加未读数
     */
    void updateOrCreateConversation(Long userId, Long targetId, Integer chatType, Long lastMsgId, boolean incrementUnread);
    
    /**
     * 隐藏会话
     * @param conversationId 会话ID
     */
    void hideConversation(Long conversationId);
    
    /**
     * 根据用户和目标隐藏会话
     * @param userId 用户ID
     * @param targetId 对方ID
     * @param chatType 聊天类型
     */
    void hideConversationByUserAndTarget(Long userId, Long targetId, Integer chatType);
    
    /**
     * 显示会话（当再次发消息时自动显示）
     * @param userId 用户ID
     * @param targetId 对方ID
     * @param chatType 聊天类型
     */
    void showConversation(Long userId, Long targetId, Integer chatType);
}
