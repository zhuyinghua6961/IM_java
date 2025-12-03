package com.im.user.service;

import com.im.user.dto.FriendRequestDTO;
import com.im.user.dto.FriendRemarkDTO;
import com.im.user.entity.FriendRequest;
import com.im.user.vo.BlacklistVO;
import com.im.user.vo.FriendVO;
import java.util.List;

public interface FriendService {
    /**
     * 获取好友列表
     * @return 好友列表
     */
    List<FriendVO> getFriendList();
    
    /**
     * 发送好友申请
     * @param dto 好友申请DTO
     * @return 好友申请记录
     */
    FriendRequest addFriendRequest(FriendRequestDTO dto);

    /**
     * 获取好友申请列表
     * @return 好友申请列表
     */
    List<FriendRequest> getFriendRequestList();
    
    /**
     * 处理好友申请
     * @param dto 好友申请DTO（包含requestId和status）
     * @return 处理后的好友申请记录
     */
    FriendRequest handleFriendRequest(FriendRequestDTO dto);

    /**
     * 更新好友备注
     * @param dto 备注更新请求
     */
    void updateRemark(FriendRemarkDTO dto);

    void deleteFriend(Long friendId);
    
    /**
     * 拉黑用户
     * @param targetUserId 被拉黑的用户ID
     */
    void blockUser(Long targetUserId);
    
    /**
     * 取消拉黑
     * @param targetUserId 被拉黑的用户ID
     */
    void unblockUser(Long targetUserId);
    
    /**
     * 获取黑名单列表
     * @return 黑名单列表
     */
    List<BlacklistVO> getBlacklist();
    
    /**
     * 检查是否被拉黑（双向）
     * @param targetUserId 目标用户ID
     * @return 是否被拉黑
     */
    boolean isBlocked(Long targetUserId);
    
    /**
     * 内部检查：指定用户是否拉黑了目标用户（单向）
     * @param blockerId 拉黑方用户ID
     * @param blockedId 被拉黑方用户ID
     * @return 是否被拉黑
     */
    boolean checkBlockedInternal(Long blockerId, Long blockedId);
}
