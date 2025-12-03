package com.im.message.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 黑名单Mapper（消息服务用于检查黑名单）
 */
@Mapper
public interface BlacklistMapper {
    
    /**
     * 检查是否被拉黑（双向检查）
     * 返回拉黑记录数，>0表示存在拉黑关系
     */
    @Select("SELECT COUNT(*) FROM blacklist " +
            "WHERE status = 1 " +
            "AND ((user_id = #{userId} AND blocked_user_id = #{targetUserId}) " +
            "OR (user_id = #{targetUserId} AND blocked_user_id = #{userId}))")
    int checkBlocked(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
    
    /**
     * 检查某用户是否拉黑了目标用户（单向）
     */
    @Select("SELECT COUNT(*) FROM blacklist " +
            "WHERE status = 1 AND user_id = #{userId} AND blocked_user_id = #{targetUserId}")
    int isBlockedBy(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
}
