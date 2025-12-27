package com.im.ai.mapper;

import com.im.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI会话Mapper
 */
@Mapper
public interface AiConversationMapper {

    /**
     * 插入会话
     */
    int insert(AiConversation conversation);

    /**
     * 更新会话标题和消息数
     */
    int updateTitleAndCount(@Param("id") Long id,
                            @Param("title") String title,
                            @Param("messageCount") Integer messageCount);

    /**
     * 查询用户的会话列表
     */
    List<AiConversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询会话详情
     */
    AiConversation selectById(@Param("id") Long id);

    /**
     * 删除会话（软删除）
     */
    int deleteById(@Param("id") Long id);
}
