package com.im.user.mapper;

import com.im.user.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    /**
     * 插入好友申请
     */
    int insert(FriendRequest friendRequest);
    
    /**
     * 根据ID查询好友申请
     */
    FriendRequest selectById(@Param("id") Long id);
    
    /**
     * 查询两个用户之间的待处理申请
     */
    FriendRequest selectPendingRequest(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
    
    /**
     * 更新申请状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 查询用户收到的好友申请列表
     */
    List<FriendRequest> selectReceivedRequests(@Param("toUserId") Long toUserId);
    
    /**
     * 根据接收方ID查询好友申请列表（别名方法）
     */
    default List<FriendRequest> selectByToUserId(Long toUserId) {
        return selectReceivedRequests(toUserId);
    }
}
