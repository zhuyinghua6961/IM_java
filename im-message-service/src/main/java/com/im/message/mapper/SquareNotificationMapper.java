package com.im.message.mapper;

import com.im.message.entity.SquareNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 广场通知 Mapper
 */
@Mapper
public interface SquareNotificationMapper {

    /**
     * 插入一条广场通知
     */
    int insert(SquareNotification notification);

    /**
     * 按用户查询广场通知历史（按时间倒序，支持分页）
     */
    List<SquareNotification> selectByUserId(@Param("userId") Long userId,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    /**
     * 统计用户的广场通知数量
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 将用户的所有广场通知标记为已读
     */
    int markAllReadByUserId(@Param("userId") Long userId);

    /**
     * 将当前用户指定的广场通知标记为已读
     */
    int markReadByUserAndIds(@Param("userId") Long userId,
                             @Param("ids") List<Long> ids);

    /**
     * 统计用户未读的广场通知数量
     */
    int countUnreadByUserId(@Param("userId") Long userId);
}
