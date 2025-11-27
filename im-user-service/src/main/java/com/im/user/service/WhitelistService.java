package com.im.user.service;

import com.im.user.vo.WhitelistVO;
import java.util.List;

/**
 * 白名单服务
 */
public interface WhitelistService {
    /**
     * 添加好友到白名单
     * @param friendId 好友ID
     */
    void addToWhitelist(Long friendId);
    
    /**
     * 从白名单移除好友
     * @param friendId 好友ID
     */
    void removeFromWhitelist(Long friendId);
    
    /**
     * 获取白名单列表
     * @return 白名单好友列表
     */
    List<WhitelistVO> getWhitelistFriends();
    
    /**
     * 检查好友是否在白名单中
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否在白名单中
     */
    boolean isInWhitelist(Long userId, Long friendId);
}
