package com.im.ai.mapper;

import com.im.ai.entity.AiChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI客服聊天记录Mapper
 */
@Mapper
public interface AiChatHistoryMapper {

    /**
     * 保存聊天记录
     */
    int insert(AiChatHistory history);

    /**
     * 查询用户的聊天记录
     */
    List<AiChatHistory> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最近的聊天记录
     */
    List<AiChatHistory> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 删除用户的所有聊天记录
     */
    int deleteByUserId(@Param("userId") Long userId);
}
