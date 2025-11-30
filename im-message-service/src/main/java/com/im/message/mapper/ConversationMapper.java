package com.im.message.mapper;

import com.im.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper {
    
    /**
     * 插入会话
     */
    int insert(Conversation conversation);
    
    /**
     * 根据ID查询会话
     */
    Conversation selectById(@Param("id") Long id);
    
    /**
     * 查询用户的会话列表
     */
    List<Conversation> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询指定会话
     */
    Conversation selectByUserIdAndTarget(@Param("userId") Long userId,
                                        @Param("targetId") Long targetId,
                                        @Param("chatType") Integer chatType);
    
    /**
     * 更新会话
     */
    int update(Conversation conversation);
    
    /**
     * 更新最后一条消息
     */
    int updateLastMessage(@Param("id") Long id, @Param("lastMsgId") Long lastMsgId);
    
    /**
     * 增加未读数
     */
    int incrementUnreadCount(@Param("id") Long id);
    
    /**
     * 清空未读数
     */
    int clearUnreadCount(@Param("id") Long id);
    
    /**
     * 设置置顶
     */
    int updateTop(@Param("id") Long id, @Param("top") Integer top);
    
    /**
     * 隐藏会话
     */
    int hideConversation(@Param("conversationId") Long conversationId);
    
    /**
     * 显示会话
     */
    int showConversation(@Param("conversationId") Long conversationId);
    
    /**
     * 根据用户ID和目标ID显示会话
     */
    int showConversationByUserAndTarget(@Param("userId") Long userId,
                                       @Param("targetId") Long targetId,
                                       @Param("chatType") Integer chatType);
    
    /**
     * 删除会话
     */
    int deleteById(@Param("id") Long id);
}
