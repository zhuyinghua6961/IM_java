package com.im.user.service;

import java.util.List;

import com.im.user.dto.GroupDTO;
import com.im.user.vo.GroupInvitationVO;
import com.im.user.vo.GroupMemberVO;
import com.im.user.vo.GroupVO;

/**
 * 群组服务
 */
public interface GroupService {
    /**
     * 创建群组
     * @param groupDTO 群组信息
     * @return 群组VO
     */
    GroupVO createGroup(GroupDTO groupDTO);

    List<GroupVO> getGroupList();

    GroupVO getGroupById(Long groupId);

    /**
     * 邀请用户加入群组（基础版：仅群主/管理员）
     * @param groupId 群组ID
     * @param userIds 被邀请人ID列表
     * @return 群组信息
     */
    GroupVO inviteGroup(Long groupId, List<Long> userIds);
    
    /**
     * 处理群组邀请（同意/拒绝）
     * @param invitationId 邀请ID
     * @param accept 是否接受
     */
    void handleInvitation(Long invitationId, boolean accept);
    
    /**
     * 获取我收到的群组邀请列表
     * @return 邀请列表
     */
    List<GroupInvitationVO> getMyInvitations();
    
    /**
     * 退出群组
     * @param groupId 群组ID
     */
    void quitGroup(Long groupId);
    
    /**
     * 设置/取消管理员
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param isAdmin 是否设为管理员（true=设为管理员，false=取消管理员）
     */
    void setGroupAdmin(Long groupId, Long userId, boolean isAdmin);
    
    /**
     * 获取群组成员列表（包含角色信息）
     * @param groupId 群组ID
     * @return 成员列表
     */
    List<GroupMemberVO> getGroupMembers(Long groupId);
    
    /**
     * 管理员审批邀请申请
     * @param invitationId 邀请ID
     * @param approve 是否批准
     */
    void approveInvitation(Long invitationId, boolean approve);
    
    /**
     * 解散群组（群主专用）
     * @param groupId 群组ID
     */
    void dissolveGroup(Long groupId);
    
    /**
     * 移除群成员（管理员/群主专用）
     * @param groupId 群组ID
     * @param userId 要移除的用户ID
     */
    void removeMember(Long groupId, Long userId);
    
    /**
     * 修改群组信息（群主专用）
     * @param groupId 群组ID
     * @param groupDTO 群组信息
     * @return 更新后的群组信息
     */
    GroupVO updateGroup(Long groupId, GroupDTO groupDTO);

    /**
     * 设置当前用户在群内的个人昵称
     * @param groupId 群组ID
     * @param nickname 群昵称（为空或空字符串表示清除昵称）
     */
    void updateMyGroupNickname(Long groupId, String nickname);

    /**
     * 转让群主（群主专用）
     * @param groupId 群组ID
     * @param newOwnerId 新群主的用户ID
     */
    void transferOwner(Long groupId, Long newOwnerId);
    
    /**
     * 设置群免打扰
     * @param groupId 群组ID
     * @param muted 是否免打扰
     */
    void setGroupMuted(Long groupId, boolean muted);
    
    /**
     * 获取群免打扰状态
     * @param groupId 群组ID
     * @return 是否免打扰
     */
    boolean isGroupMuted(Long groupId);
}
