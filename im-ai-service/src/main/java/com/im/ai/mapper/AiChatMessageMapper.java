package com.im.ai.mapper;

import com.im.ai.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI聊天消息Mapper
 */
@Mapper
public interface AiChatMessageMapper {

    /**
     * 插入消息
     */
    int insert(AiChatMessage message);

    /**
     * 查询会话的消息列表
     */
    List<AiChatMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 统计会话消息数量
     */
    int countByConversationId(@Param("conversationId") Long conversationId);
}
