package com.im.square.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 广场模块内部使用的好友关系只读查询
 */
@Mapper
public interface FriendRelationMapper {

    /**
     * 统计 userId 与 friendId 之间的有效好友关系数量
     */
    int countFriendship(@Param("userId") Long userId,
                        @Param("friendId") Long friendId);
}
