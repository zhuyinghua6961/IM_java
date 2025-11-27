package com.im.user.mapper;

import com.im.user.entity.UserWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 白名单Mapper
 */
@Mapper
public interface WhitelistMapper {
    /**
     * 添加到白名单
     */
    int insert(UserWhitelist whitelist);
    
    /**
     * 从白名单移除
     */
    int delete(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 查询用户的白名单列表
     */
    List<Long> selectWhitelistFriendIds(@Param("userId") Long userId);
    
    /**
     * 检查好友是否在白名单中
     */
    boolean isInWhitelist(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 查询白名单数量
     */
    int countByUserId(@Param("userId") Long userId);
}
