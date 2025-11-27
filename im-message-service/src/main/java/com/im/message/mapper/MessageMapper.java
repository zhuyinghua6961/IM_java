package com.im.message.mapper;

import com.im.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    
    /**
     * 插入消息
     */
    int insert(Message message);
    
    /**
     * 根据ID查询消息
     */
    Message selectById(@Param("id") Long id);
    
    /**
     * 查询历史消息（分页）
     */
    List<Message> selectHistoryMessages(@Param("userId") Long userId,
                                       @Param("targetId") Long targetId,
                                       @Param("chatType") Integer chatType,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);
    
    /**
     * 统计历史消息总数
     */
    int countHistoryMessages(@Param("userId") Long userId,
                            @Param("targetId") Long targetId,
                            @Param("chatType") Integer chatType);
    
    /**
     * 更新消息状态（撤回）
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 批量插入已读记录
     */
    int batchInsertReadRecords(@Param("messageIds") List<Long> messageIds,
                              @Param("userId") Long userId);
    
    /**
     * 撤回消息
     */
    int recallMessage(@Param("messageId") Long messageId, 
                     @Param("fromUserId") Long fromUserId);
}
