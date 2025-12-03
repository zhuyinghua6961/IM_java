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
    
    /**
     * 删除用户的聊天记录
     * @param userId 用户ID
     * @param targetId 对方ID（用户ID或群组ID）
     * @param chatType 聊天类型
     */
    int deleteUserMessages(@Param("userId") Long userId,
                          @Param("targetId") Long targetId,
                          @Param("chatType") Integer chatType);
    
    /**
     * 根据ID删除消息（物理删除，用于撤回补偿）
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新消息（用于修改状态等）
     */
    int updateById(Message message);
    
    /**
     * 更新消息撤回状态
     */
    int updateRecallStatus(@Param("messageId") Long messageId,
                          @Param("recallTime") Long recallTime);
    
    /**
     * 搜索消息
     */
    List<Message> searchMessages(@Param("userId") Long userId,
                                 @Param("keyword") String keyword,
                                 @Param("groupIds") List<Long> groupIds,
                                 @Param("chatType") Integer chatType,
                                 @Param("targetId") Long targetId,
                                 @Param("startTime") java.util.Date startTime,
                                 @Param("endTime") java.util.Date endTime,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);
    
    /**
     * 统计搜索结果数量
     */
    int countSearchMessages(@Param("userId") Long userId,
                           @Param("keyword") String keyword,
                           @Param("groupIds") List<Long> groupIds,
                           @Param("chatType") Integer chatType,
                           @Param("targetId") Long targetId,
                           @Param("startTime") java.util.Date startTime,
                           @Param("endTime") java.util.Date endTime);
    
    /**
     * 查询消息上下文（指定消息前后的消息）
     */
    List<Message> selectMessageContext(@Param("userId") Long userId,
                                       @Param("targetId") Long targetId,
                                       @Param("chatType") Integer chatType,
                                       @Param("messageId") Long messageId,
                                       @Param("contextSize") Integer contextSize);
}
