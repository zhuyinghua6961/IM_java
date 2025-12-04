package com.im.message.mapper;

import com.im.message.entity.GroupNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群消息通知 Mapper
 */
@Mapper
public interface GroupNotificationMapper {

    /**
     * 插入一条群通知
     */
    int insert(GroupNotification notification);

    /**
     * 按用户查询群通知历史（按时间倒序，支持分页）
     */
    List<GroupNotification> selectByUserId(@Param("userId") Long userId,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);

    /**
     * 统计用户的群通知数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 将用户的所有群通知标记为已读
     */
    int markAllReadByUserId(@Param("userId") Long userId);
}
