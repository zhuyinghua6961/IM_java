package com.im.message.mapper;

import com.im.message.entity.MessageDelete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageDeleteMapper {
    
    /**
     * 插入删除记录
     */
    int insert(MessageDelete messageDelete);
    
    /**
     * 批量插入删除记录
     */
    int batchInsert(@Param("list") List<MessageDelete> list);
    
    /**
     * 查询用户删除的消息ID列表
     */
    List<Long> selectDeletedMessageIds(@Param("userId") Long userId,
                                       @Param("targetId") Long targetId,
                                       @Param("chatType") Integer chatType);
    
    /**
     * 检查消息是否被用户删除
     */
    int checkDeleted(@Param("userId") Long userId, @Param("messageId") Long messageId);
    
    /**
     * 查询用户对某条消息的删除记录
     */
    MessageDelete selectByUserAndMessage(@Param("userId") Long userId, @Param("messageId") Long messageId);
}
