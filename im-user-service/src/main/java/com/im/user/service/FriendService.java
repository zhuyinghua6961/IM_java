package com.im.user.service;

import com.im.user.dto.FriendRequestDTO;
import com.im.user.entity.FriendRequest;
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

    void deleteFriend(Long friendId);
}
