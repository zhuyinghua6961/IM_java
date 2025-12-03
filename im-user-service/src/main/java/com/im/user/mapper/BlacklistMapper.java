package com.im.user.mapper;

import com.im.user.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 黑名单Mapper
 */
@Mapper
public interface BlacklistMapper {
    
    /**
     * 插入黑名单记录
     */
    int insert(Blacklist blacklist);
    
    /**
     * 根据ID查询
     */
    Blacklist selectById(@Param("id") Long id);
    
    /**
     * 查询用户的黑名单列表
     */
    List<Blacklist> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询特定的黑名单记录
     */
    Blacklist selectByUserIdAndBlockedUserId(@Param("userId") Long userId, 
                                              @Param("blockedUserId") Long blockedUserId);
    
    /**
     * 检查是否在黑名单中（包括双向检查）
     */
    Blacklist checkBlocked(@Param("userId") Long userId, 
                           @Param("targetUserId") Long targetUserId);
    
    /**
     * 更新黑名单状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 更新黑名单记录
     */
    int updateById(Blacklist blacklist);
}
