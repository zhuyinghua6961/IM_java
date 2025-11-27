package com.im.user.service;

import com.im.user.entity.GroupMember;

import java.util.List;

/**
 * 群成员服务
 */
public interface GroupMemberService {
    
    /**
     * 查询群成员列表
     * @param groupId 群组ID
     * @return 群成员列表
     */
    List<GroupMember> getGroupMembers(Long groupId);
    
    /**
     * 查询用户在某个群的成员信息
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 成员信息，不存在返回null
     */
    GroupMember getGroupMember(Long groupId, Long userId);
    
    /**
     * 检查用户是否是群成员
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return true-是群成员，false-不是
     */
    boolean isMember(Long groupId, Long userId);
    
    /**
     * 获取群成员ID列表（排除指定用户）
     * @param groupId 群组ID
     * @param excludeUserId 要排除的用户ID
     * @return 群成员ID列表
     */
    List<Long> getGroupMemberIds(Long groupId, Long excludeUserId);
}
