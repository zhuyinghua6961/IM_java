package com.im.user.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.user.context.UserContext;
import com.im.user.dto.GroupDTO;
import com.im.user.entity.Group;
import com.im.user.entity.GroupInvitation;
import com.im.user.entity.GroupMember;
import com.im.user.entity.User;
import com.im.user.mapper.GroupInvitationMapper;
import com.im.user.mapper.GroupMapper;
import com.im.user.mapper.GroupMemberMapper;
import com.im.user.mapper.UserMapper;
import com.im.user.mapper.WhitelistMapper;
import com.im.user.service.GroupService;
import com.im.user.vo.GroupInvitationVO;
import com.im.user.vo.GroupMemberVO;
import com.im.user.vo.GroupVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {
    
    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Autowired
    private WhitelistMapper whitelistMapper;
    
    @Autowired
    private GroupInvitationMapper groupInvitationMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private com.im.user.utils.WebSocketUtil webSocketUtil;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(GroupDTO groupDTO) {
        // 1. 获取当前用户ID（创建者/群主）
        Long ownerId = UserContext.getCurrentUserId();
        log.info("创建群组，ownerId: {}, groupName: {}", ownerId, groupDTO.getGroupName());
        
        // 2. 参数验证
        if (groupDTO.getGroupName() == null || groupDTO.getGroupName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群名称不能为空");
        }
        
        // 设置默认最大成员数
        Integer maxMembers = groupDTO.getMaxMembers() != null ? groupDTO.getMaxMembers() : 500;
        
        // 验证初始成员数量
        int initialMemberCount = 1; // 群主
        if (groupDTO.getMemberIds() != null) {
            initialMemberCount += groupDTO.getMemberIds().size();
        }
        if (initialMemberCount > maxMembers) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "初始成员数量超过群最大人数限制");
        }
        
        // 3. 创建群组记录
        Group group = new Group();
        group.setGroupName(groupDTO.getGroupName());
        group.setAvatar(groupDTO.getAvatar());
        group.setOwnerId(ownerId);
        group.setNotice(groupDTO.getNotice());
        group.setMaxMembers(maxMembers);
        group.setStatus(1); // 1-正常
        groupMapper.insert(group);
        
        Long groupId = group.getId();
        log.info("群组创建成功，groupId: {}", groupId);
        
        // 4. 添加群主到群成员表
        GroupMember owner = new GroupMember();
        owner.setGroupId(groupId);
        owner.setUserId(ownerId);
        owner.setRole(2); // 2-群主
        owner.setJoinTime(LocalDateTime.now());
        owner.setStatus(1);
        groupMemberMapper.insert(owner);
        
        // 5. 处理初始成员（基于白名单策略）
        int addedCount = 1; // 已添加群主
        if (groupDTO.getMemberIds() != null && !groupDTO.getMemberIds().isEmpty()) {
            // 去重并排除群主本人
            Set<Long> uniqueMemberIds = new HashSet<>(groupDTO.getMemberIds());
            uniqueMemberIds.remove(ownerId); // 移除群主ID（避免重复添加）
            
            // 分类处理：白名单成员 vs 非白名单成员
            List<GroupMember> directAddMembers = new ArrayList<>();      // 直接添加的成员
            List<Long> needInvitationMembers = new ArrayList<>();        // 需要发送邀请的成员
            
            for (Long memberId : uniqueMemberIds) {
                // 查询该成员是否在我的白名单中（注意：是memberId设置的白名单，允许ownerId拉他）
                // 逻辑：如果B在A的白名单中，说明B信任A，A可以直接拉B进群
                boolean isInWhitelist = whitelistMapper.isInWhitelist(memberId, ownerId);
                
                if (isInWhitelist) {
                    // 在白名单中：直接添加
                    GroupMember member = new GroupMember();
                    member.setGroupId(groupId);
                    member.setUserId(memberId);
                    member.setRole(0); // 0-普通成员
                    member.setJoinTime(LocalDateTime.now());
                    member.setStatus(1);
                    directAddMembers.add(member);
                } else {
                    // 不在白名单中：需要发送邀请
                    needInvitationMembers.add(memberId);
                }
            }
            
            // 批量插入白名单成员
            if (!directAddMembers.isEmpty()) {
                groupMemberMapper.batchInsert(directAddMembers);
                addedCount += directAddMembers.size();
                log.info("直接添加白名单成员，groupId: {}, count: {}", groupId, directAddMembers.size());
            }
            
            // 为非白名单成员发送群组邀请
            if (!needInvitationMembers.isEmpty()) {
                List<GroupInvitation> invitations = new ArrayList<>();
                for (Long memberId : needInvitationMembers) {
                    GroupInvitation invitation = new GroupInvitation();
                    invitation.setGroupId(groupId);
                    invitation.setInviterId(ownerId);
                    invitation.setInviteeId(memberId);
                    invitation.setInviterRole(2); // 群主
                    invitation.setStatus(1); // 待被邀请人同意
                    invitation.setExpireTime(LocalDateTime.now().plusDays(7));
                    invitations.add(invitation);
                }
                
                // 批量插入邀请记录
                groupInvitationMapper.batchInsert(invitations);
                log.info("创建群组邀请记录成功，groupId: {}, count: {}", groupId, invitations.size());
                
                // 推送通知给被邀请人
                User ownerUser = userMapper.selectById(ownerId);
                String ownerName = ownerUser != null ? ownerUser.getNickname() : "未知用户";
                
                for (GroupInvitation invitation : invitations) {
                    webSocketUtil.pushGroupInvitation(
                        invitation.getInviteeId(),
                        ownerId,
                        ownerName,
                        groupId,
                        group.getGroupName(),
                        invitation.getId()
                    );
                }
            }
        }
        
        log.info("群成员添加成功，groupId: {}, memberCount: {}", groupId, addedCount);
        
        // 6. 组装返回VO
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupId(groupId);
        groupVO.setGroupName(group.getGroupName());
        groupVO.setAvatar(group.getAvatar());
        groupVO.setOwnerId(ownerId);
        groupVO.setNotice(group.getNotice());
        groupVO.setMemberCount(addedCount);
        groupVO.setMaxMembers(maxMembers);
        groupVO.setStatus(1);
        groupVO.setCreateTime(group.getCreateTime());
        
        return groupVO;
    }


    @Override
    public List<GroupVO> getGroupList() {
        Long userId = UserContext.getCurrentUserId();
        if(userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        List<Group> groups = groupMapper.selectByUserId(userId);
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<GroupVO> groupVOList = new ArrayList<>();
        for (Group group : groups) {
            GroupVO groupVO = new GroupVO();
            groupVO.setGroupId(group.getId());
            groupVO.setGroupName(group.getGroupName());
            groupVO.setAvatar(group.getAvatar());
            groupVO.setOwnerId(group.getOwnerId());
            groupVO.setNotice(group.getNotice());
            groupVO.setMaxMembers(group.getMaxMembers());
            groupVO.setStatus(group.getStatus());
            groupVO.setCreateTime(group.getCreateTime());
            
            // 查询群成员数量
            int memberCount = groupMemberMapper.countByGroupId(group.getId());
            groupVO.setMemberCount(memberCount);
            
            groupVOList.add(groupVO);
        }
        return groupVOList;
    }

    @Override
    public GroupVO getGroupById(Long groupId){
        Long userId = UserContext.getCurrentUserId();
        log.info("获取群组详情，groupId: {}, userId: {}", groupId, userId);
        
        if(userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        Group group = groupMapper.selectById(groupId);
        if(group == null) {
            log.warn("群组不存在，groupId: {}", groupId);
            throw new BusinessException(ResultCode.NOT_FOUND, "群组不存在");
        }
        
        // 检查用户是否是群成员
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        log.info("查询群成员结果，member: {}", member);
        
        if(member == null || member.getStatus() != 1) {
            log.warn("用户不是群成员或状态异常，userId: {}, groupId: {}, member: {}", userId, groupId, member);
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该群成员，无权访问");
        }
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupId(group.getId());
        groupVO.setGroupName(group.getGroupName());
        groupVO.setAvatar(group.getAvatar());
        groupVO.setOwnerId(group.getOwnerId());
        groupVO.setNotice(group.getNotice());
        groupVO.setMaxMembers(group.getMaxMembers());
        groupVO.setStatus(group.getStatus());
        groupVO.setCreateTime(group.getCreateTime());
        
        // 查询群成员数量
        int memberCount = groupMemberMapper.countByGroupId(group.getId());
        groupVO.setMemberCount(memberCount);
        return groupVO;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO inviteGroup(Long groupId, List<Long> userIds) {
        // 1. 获取当前用户ID
        Long inviterId = UserContext.getCurrentUserId();
        log.info("邀请用户加入群组，groupId: {}, inviterId: {}, userIds: {}", groupId, inviterId, userIds);
        
        if (inviterId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 校验群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            log.warn("群组不存在，groupId: {}", groupId);
            throw new BusinessException(ResultCode.NOT_FOUND, "群组不存在");
        }
        
        // 3. 校验邀请人权限（必须是群成员）
        GroupMember inviter = groupMemberMapper.selectByGroupIdAndUserId(groupId, inviterId);
        if (inviter == null || inviter.getStatus() != 1) {
            log.warn("用户不是群成员，userId: {}, groupId: {}", inviterId, groupId);
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是群成员，无权邀请");
        }
        
        // 4. 批量处理邀请
        List<GroupInvitation> invitations = new ArrayList<>();
        List<GroupMember> directJoinMembers = new ArrayList<>(); // 直接加入的成员
        
        for (Long inviteeId : userIds) {
            // 4.1 跳过自己
            if (inviteeId.equals(inviterId)) {
                continue;
            }
            
            // 4.2 检查是否已经是群成员
            GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, inviteeId);
            if (existingMember != null && existingMember.getStatus() == 1) {
                log.info("用户已是群成员，跳过，inviteeId: {}", inviteeId);
                continue;
            }
            
            // 4.3 检查是否已有待处理邀请
            GroupInvitation pending = groupInvitationMapper.selectPendingInvitation(groupId, inviteeId);
            if (pending != null) {
                log.info("用户已有待处理邀请，跳过，inviteeId: {}", inviteeId);
                continue;
            }
            
            // 4.4 检查白名单关系
            boolean isInWhitelist = whitelistMapper.isInWhitelist(inviteeId, inviterId);
            log.info("白名单检查结果，invitee: {}, inviter: {}, inWhitelist: {}", inviteeId, inviterId, isInWhitelist);
            
            // 4.5 根据邀请人角色和白名单关系决定处理方式
            if (inviter.getRole() >= 1) {
                // 管理员/群主邀请
                if (isInWhitelist) {
                    // 白名单用户直接加入
                    // 检查是否已存在成员记录（可能之前被移除过）
                    GroupMember whitelistMember = groupMemberMapper.selectByGroupIdAndUserIdIncludeInactive(groupId, inviteeId);
                    if (whitelistMember != null && whitelistMember.getStatus() == 0) {
                        // 已存在但已退出，重新激活
                        whitelistMember.setStatus(1);
                        whitelistMember.setRole(0);
                        whitelistMember.setJoinTime(LocalDateTime.now());
                        whitelistMember.setUpdateTime(LocalDateTime.now());
                        groupMemberMapper.updateById(whitelistMember);
                        log.info("管理员邀请白名单用户，重新激活成员记录，inviteeId: {}", inviteeId);
                        
                        // 通知用户直接加入
                        User inviterUser = userMapper.selectById(inviterId);
                        String inviterName = inviterUser != null ? inviterUser.getNickname() : "未知用户";
                        webSocketUtil.pushGroupJoinNotification(
                            inviteeId,
                            inviterId,
                            inviterName,
                            groupId,
                            group.getGroupName()
                        );
                    } else if (whitelistMember == null) {
                        // 不存在记录，添加到待插入列表
                        GroupMember newMember = new GroupMember();
                        newMember.setGroupId(groupId);
                        newMember.setUserId(inviteeId);
                        newMember.setRole(0); // 普通成员
                        newMember.setJoinTime(LocalDateTime.now());
                        newMember.setStatus(1);
                        directJoinMembers.add(newMember);
                        log.info("管理员邀请白名单用户，直接加入，inviteeId: {}", inviteeId);
                    }
                    // 如果whitelistMember存在且status=1，说明已经是群成员，跳过
                } else {
                    // 非白名单用户发送邀请
                    GroupInvitation invitation = new GroupInvitation();
                    invitation.setGroupId(groupId);
                    invitation.setInviterId(inviterId);
                    invitation.setInviteeId(inviteeId);
                    invitation.setInviterRole(inviter.getRole());
                    invitation.setStatus(1); // 待被邀请人同意
                    invitation.setExpireTime(LocalDateTime.now().plusDays(7));
                    invitations.add(invitation);
                    log.info("管理员邀请非白名单用户，发送邀请，inviteeId: {}", inviteeId);
                }
            } else {
                // 普通成员邀请，需要管理员审批
                GroupInvitation invitation = new GroupInvitation();
                invitation.setGroupId(groupId);
                invitation.setInviterId(inviterId);
                invitation.setInviteeId(inviteeId);
                invitation.setInviterRole(inviter.getRole());
                invitation.setStatus(0); // 待管理员审批
                invitation.setExpireTime(LocalDateTime.now().plusDays(7));
                invitations.add(invitation);
                log.info("普通成员邀请，需要管理员审批，inviteeId: {}", inviteeId);
            }
        }
        
        // 5. 处理直接加入的成员
        if (!directJoinMembers.isEmpty()) {
            groupMemberMapper.batchInsert(directJoinMembers);
            log.info("白名单用户直接加入成功，count: {}", directJoinMembers.size());
            
            // 通知直接加入的用户
            User inviterUser = userMapper.selectById(inviterId);
            String inviterName = inviterUser != null ? inviterUser.getNickname() : "未知用户";
            
            for (GroupMember member : directJoinMembers) {
                webSocketUtil.pushGroupJoinNotification(
                    member.getUserId(),
                    inviterId,
                    inviterName,
                    groupId,
                    group.getGroupName()
                );
            }
        }
        
        // 6. 批量插入邀请记录
        if (!invitations.isEmpty()) {
            groupInvitationMapper.batchInsert(invitations);
            log.info("创建邀请记录成功，count: {}", invitations.size());
            
            User inviterUser = userMapper.selectById(inviterId);
            String inviterName = inviterUser != null ? inviterUser.getNickname() : "未知用户";
            
            // 7. 根据邀请状态发送不同通知
            for (GroupInvitation invitation : invitations) {
                if (invitation.getStatus() == 0) {
                    // 待管理员审批，通知管理员
                    List<GroupMember> admins = groupMemberMapper.selectAdminsByGroupId(groupId);
                    for (GroupMember admin : admins) {
                        webSocketUtil.pushGroupInviteApprovalNotification(
                            admin.getUserId(),
                            inviterId,
                            inviterName,
                            invitation.getInviteeId(),
                            groupId,
                            group.getGroupName(),
                            invitation.getId()
                        );
                    }
                } else if (invitation.getStatus() == 1) {
                    // 直接邀请，通知被邀请人
                    webSocketUtil.pushGroupInvitation(
                        invitation.getInviteeId(),
                        inviterId,
                        inviterName,
                        groupId,
                        group.getGroupName(),
                        invitation.getId()
                    );
                }
            }
        }
        
        // 8. 返回群组信息
        return getGroupById(groupId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInvitation(Long invitationId, boolean accept) {
        // 1. 获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("处理群组邀请，invitationId: {}, userId: {}, accept: {}", invitationId, userId, accept);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询邀请记录
        GroupInvitation invitation = groupInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请记录不存在");
        }
        
        // 3. 校验权限（必须是被邀请人本人）
        if (!invitation.getInviteeId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此邀请");
        }
        
        // 4. 校验邀请状态
        if (invitation.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请已处理或已过期");
        }
        
        // 5. 检查是否过期
        if (LocalDateTime.now().isAfter(invitation.getExpireTime())) {
            invitation.setStatus(5); // 已过期
            groupInvitationMapper.updateById(invitation);
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请已过期");
        }
        
        // 6. 获取相关信息用于推送通知
        Group group = groupMapper.selectById(invitation.getGroupId());
        User inviteeUser = userMapper.selectById(userId);
        String inviteeName = inviteeUser != null ? inviteeUser.getNickname() : "未知用户";
        String groupName = group != null ? group.getGroupName() : "未知群组";
        
        if (accept) {
            // 7. 同意：加入群组
            // 检查是否已存在成员记录（可能之前被移除过）
            GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserIdIncludeInactive(invitation.getGroupId(), userId);
            
            if (existingMember != null) {
                // 已存在记录，更新状态（重新激活）
                existingMember.setStatus(1);
                existingMember.setRole(0); // 重置为普通成员
                existingMember.setJoinTime(LocalDateTime.now()); // 更新加入时间
                existingMember.setUpdateTime(LocalDateTime.now());
                groupMemberMapper.updateById(existingMember);
                log.info("重新激活群成员记录，userId: {}, groupId: {}", userId, invitation.getGroupId());
            } else {
                // 不存在记录，插入新记录
                GroupMember member = new GroupMember();
                member.setGroupId(invitation.getGroupId());
                member.setUserId(userId);
                member.setRole(0); // 普通成员
                member.setStatus(1);
                member.setJoinTime(LocalDateTime.now());
                groupMemberMapper.insert(member);
                log.info("插入新群成员记录，userId: {}, groupId: {}", userId, invitation.getGroupId());
            }
            
            // 更新邀请状态
            invitation.setStatus(2); // 已同意
            invitation.setInviteeReplyTime(LocalDateTime.now());
            groupInvitationMapper.updateById(invitation);
            
            log.info("用户同意加入群组，userId: {}, groupId: {}", userId, invitation.getGroupId());
            
            // 通知群主和管理员有新成员加入
            List<GroupMember> admins = groupMemberMapper.selectAdminsByGroupId(invitation.getGroupId());
            for (GroupMember admin : admins) {
                webSocketUtil.pushGroupNewMemberNotification(
                    admin.getUserId(),
                    userId,
                    inviteeName,
                    invitation.getGroupId(),
                    groupName
                );
            }
        } else {
            // 8. 拒绝
            invitation.setStatus(4); // 被邀请人拒绝
            invitation.setInviteeReplyTime(LocalDateTime.now());
            groupInvitationMapper.updateById(invitation);
            
            log.info("用户拒绝加入群组，userId: {}, groupId: {}", userId, invitation.getGroupId());
        }
        
        // 9. 推送处理结果通知给邀请人
        webSocketUtil.pushGroupInvitationResult(
            invitation.getInviterId(),
            userId,
            inviteeName,
            invitation.getGroupId(),
            groupName,
            accept
        );
    }
    
    @Override
    public List<GroupInvitationVO> getMyInvitations() {
        // 1. 获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("获取我的群组邀请列表，userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询待处理的邀请
        List<GroupInvitation> invitations = groupInvitationMapper.selectPendingByInviteeId(userId);
        
        // 3. 组装VO
        List<GroupInvitationVO> result = new ArrayList<>();
        for (GroupInvitation invitation : invitations) {
            GroupInvitationVO vo = new GroupInvitationVO();
            vo.setId(invitation.getId());
            vo.setGroupId(invitation.getGroupId());
            vo.setInviterId(invitation.getInviterId());
            vo.setStatus(invitation.getStatus());
            vo.setExpireTime(invitation.getExpireTime());
            vo.setCreateTime(invitation.getCreateTime());
            
            // 查询群组信息
            Group group = groupMapper.selectById(invitation.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getGroupName());
                vo.setGroupAvatar(group.getAvatar());
            }
            
            // 查询邀请人信息
            User inviter = userMapper.selectById(invitation.getInviterId());
            if (inviter != null) {
                vo.setInviterNickname(inviter.getNickname());
                vo.setInviterAvatar(inviter.getAvatar());
            }
            
            result.add(vo);
        }
        
        log.info("获取邀请列表成功，count: {}", result.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId) {
        // 1. 获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("用户退出群组，userId: {}, groupId: {}", userId, groupId);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }
        
        // 3. 查询用户在群组中的成员关系
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null || member.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您不是该群组成员");
        }
        
        // 4. 检查是否是群主
        if (member.getRole() == 2) {
            // 群主退出等同于解散群组，需要前端确认
            throw new BusinessException(ResultCode.BAD_REQUEST, "群主退出将解散群组，请使用解散群组功能");
        }
        
        // 5. 获取退出用户信息
        User quitUser = userMapper.selectById(userId);
        String quitUserName = quitUser != null ? quitUser.getNickname() : "未知用户";
        
        // 6. 退出群组（软删除成员关系）
        member.setStatus(0); // 设置为已退出状态
        member.setUpdateTime(LocalDateTime.now());
        groupMemberMapper.updateById(member);
        
        // 7. 通知群主和管理员（不通知普通成员）
        List<GroupMember> admins = groupMemberMapper.selectAdminsByGroupId(groupId);
        for (GroupMember admin : admins) {
            // 不通知退出的用户自己
            if (!admin.getUserId().equals(userId)) {
                webSocketUtil.pushGroupQuitNotification(
                    admin.getUserId(),
                    userId,
                    quitUserName,
                    groupId,
                    group.getGroupName()
                );
            }
        }
        
        log.info("用户退出群组成功，userId: {}, groupId: {}, 已通知管理员数量: {}", 
                userId, groupId, admins.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyGroupNickname(Long groupId, String nickname) {
        Long userId = UserContext.getCurrentUserId();
        log.info("更新群内个人昵称，userId: {}, groupId: {}, nickname: {}", userId, groupId, nickname);

        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }

        // 1. 校验群组存在且未解散
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群组不存在或已解散");
        }

        // 2. 校验当前用户是该群成员
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null || member.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该群成员，无法设置群昵称");
        }

        // 3. 处理空白昵称：空串当成清除昵称
        String trimmed = nickname != null ? nickname.trim() : null;
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }

        member.setNickname(trimmed);
        member.setUpdateTime(LocalDateTime.now());
        groupMemberMapper.updateById(member);

        log.info("更新群内个人昵称成功，userId: {}, groupId: {}, nickname: {}", userId, groupId, trimmed);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long groupId, Long newOwnerId) {
        Long currentUserId = UserContext.getCurrentUserId();
        log.info("转让群主，operatorId: {}, groupId: {}, newOwnerId: {}", currentUserId, groupId, newOwnerId);
        
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 1. 校验群组存在且未解散
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群组不存在或已解散");
        }
        
        // 2. 校验当前用户是群主
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主可以转让群主");
        }
        
        // 3. 校验新群主是该群有效成员
        GroupMember newOwnerMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, newOwnerId);
        if (newOwnerMember == null || newOwnerMember.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户不是该群成员");
        }
        
        // 4. 不能转让给自己
        if (newOwnerId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能转让给自己");
        }
        
        // 5. 更新群组表的 owner_id
        group.setOwnerId(newOwnerId);
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
        
        // 6. 更新原群主的角色为普通成员
        GroupMember oldOwnerMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (oldOwnerMember != null) {
            groupMemberMapper.updateRole(oldOwnerMember.getId(), 0); // 0=普通成员
        }
        
        // 7. 更新新群主的角色为群主
        groupMemberMapper.updateRole(newOwnerMember.getId(), 2); // 2=群主
        
        // 8. 获取用户信息用于推送通知
        User oldOwner = userMapper.selectById(currentUserId);
        String oldOwnerName = oldOwner != null ? oldOwner.getNickname() : "未知用户";
        
        // 9. 只通知新群主
        webSocketUtil.pushGroupOwnerTransferNotification(
            newOwnerId,
            currentUserId,
            oldOwnerName,
            groupId,
            group.getGroupName(),
            true
        );
        
        log.info("转让群主成功，groupId: {}, oldOwnerId: {}, newOwnerId: {}", groupId, currentUserId, newOwnerId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setGroupAdmin(Long groupId, Long userId, boolean isAdmin) {
        // 1. 获取当前用户ID（操作者）
        Long currentUserId = UserContext.getCurrentUserId();
        log.info("设置群组管理员，operatorId: {}, groupId: {}, targetUserId: {}, isAdmin: {}", 
                currentUserId, groupId, userId, isAdmin);
        
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }
        
        // 3. 验证操作者权限（只有群主可以设置管理员）
        GroupMember operator = groupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (operator == null || operator.getStatus() != 1 || operator.getRole() != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主可以设置管理员");
        }
        
        // 4. 查询目标用户的成员信息
        GroupMember targetMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (targetMember == null || targetMember.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户不是群组成员");
        }
        
        // 5. 检查是否是群主（群主不能被设置为管理员）
        if (targetMember.getRole() == 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能修改群主的角色");
        }
        
        // 6. 更新角色
        int newRole = isAdmin ? 1 : 0; // 1=管理员, 0=普通成员
        if (targetMember.getRole() == newRole) {
            throw new BusinessException(ResultCode.BAD_REQUEST, 
                isAdmin ? "该用户已经是管理员" : "该用户已经是普通成员");
        }
        
        groupMemberMapper.updateRole(targetMember.getId(), newRole);
        
        // 7. 发送通知给目标用户
        User targetUser = userMapper.selectById(userId);
        String targetUserName = targetUser != null ? targetUser.getNickname() : "未知用户";
        
        webSocketUtil.pushGroupAdminNotification(
            userId,
            currentUserId,
            targetUserName,
            groupId,
            group.getGroupName(),
            isAdmin
        );
        
        // 8. 通知其他管理员和群主
        List<GroupMember> admins = groupMemberMapper.selectAdminsByGroupId(groupId);
        for (GroupMember admin : admins) {
            // 排除被变更的人（已通知）和操作者（群主自己）
            if (!admin.getUserId().equals(userId) && !admin.getUserId().equals(currentUserId)) {
                webSocketUtil.pushGroupAdminNotification(
                    admin.getUserId(),
                    currentUserId,
                    targetUserName,
                    groupId,
                    group.getGroupName(),
                    isAdmin
                );
            }
        }
        
        log.info("设置群组管理员成功，groupId: {}, userId: {}, newRole: {}", groupId, userId, newRole);
    }
    
    @Override
    public List<GroupMemberVO> getGroupMembers(Long groupId) {
        // 1. 获取当前用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        log.info("获取群组成员列表，userId: {}, groupId: {}", currentUserId, groupId);
        
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 验证用户是否是群组成员
        GroupMember currentMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (currentMember == null || currentMember.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该群组成员");
        }
        
        // 3. 查询群组成员列表
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        
        // 4. 转换为 VO 并补充用户信息
        List<GroupMemberVO> result = new ArrayList<>();
        for (GroupMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            if (user != null) {
                GroupMemberVO vo = new GroupMemberVO();
                vo.setUserId(member.getUserId());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
                vo.setGroupNickname(member.getNickname());
                vo.setRole(member.getRole());
                vo.setJoinTime(member.getJoinTime());
                vo.setMuteUntil(member.getMuteUntil());
                result.add(vo);
            }
        }
        
        log.info("获取群组成员列表成功，groupId: {}, memberCount: {}", groupId, result.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveInvitation(Long invitationId, boolean approve) {
        // 1. 获取当前用户ID（管理员）
        Long adminId = UserContext.getCurrentUserId();
        log.info("管理员审批邀请，adminId: {}, invitationId: {}, approve: {}", adminId, invitationId, approve);
        
        if (adminId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询邀请记录
        GroupInvitation invitation = groupInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请记录不存在");
        }
        
        // 3. 验证邀请状态（必须是待审批状态）
        if (invitation.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请不是待审批状态");
        }
        
        // 4. 验证管理员权限
        GroupMember admin = groupMemberMapper.selectByGroupIdAndUserId(invitation.getGroupId(), adminId);
        if (admin == null || admin.getStatus() != 1 || admin.getRole() < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有管理员和群主可以审批邀请");
        }
        
        // 5. 检查是否过期
        if (LocalDateTime.now().isAfter(invitation.getExpireTime())) {
            invitation.setStatus(5); // 已过期
            groupInvitationMapper.updateById(invitation);
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请已过期");
        }
        
        // 6. 获取相关信息
        Group group = groupMapper.selectById(invitation.getGroupId());
        User inviterUser = userMapper.selectById(invitation.getInviterId());
        User inviteeUser = userMapper.selectById(invitation.getInviteeId());
        String inviterName = inviterUser != null ? inviterUser.getNickname() : "未知用户";
        String inviteeName = inviteeUser != null ? inviteeUser.getNickname() : "未知用户";
        String groupName = group != null ? group.getGroupName() : "未知群组";
        
        if (approve) {
            // 7. 批准：检查白名单关系决定下一步
            boolean isInWhitelist = whitelistMapper.isInWhitelist(invitation.getInviteeId(), invitation.getInviterId());
            
            if (isInWhitelist) {
                // 白名单用户直接加入
                // 检查是否已存在成员记录
                GroupMember existingMember = groupMemberMapper.selectByGroupIdAndUserIdIncludeInactive(
                    invitation.getGroupId(), invitation.getInviteeId());
                
                if (existingMember != null) {
                    // 已存在记录，更新状态
                    existingMember.setStatus(1);
                    existingMember.setRole(0);
                    existingMember.setJoinTime(LocalDateTime.now());
                    existingMember.setUpdateTime(LocalDateTime.now());
                    groupMemberMapper.updateById(existingMember);
                } else {
                    // 不存在记录，插入新记录
                    GroupMember member = new GroupMember();
                    member.setGroupId(invitation.getGroupId());
                    member.setUserId(invitation.getInviteeId());
                    member.setRole(0); // 普通成员
                    member.setJoinTime(LocalDateTime.now());
                    member.setStatus(1);
                    groupMemberMapper.insert(member);
                }
                
                // 更新邀请状态为已同意
                invitation.setStatus(2);
                invitation.setAdminReviewerId(adminId);
                invitation.setAdminReviewTime(LocalDateTime.now());
                invitation.setAdminReviewNote("管理员批准，白名单用户直接加入");
                groupInvitationMapper.updateById(invitation);
                
                // 通知被邀请人直接加入
                webSocketUtil.pushGroupJoinNotification(
                    invitation.getInviteeId(),
                    invitation.getInviterId(),
                    inviterName,
                    invitation.getGroupId(),
                    groupName
                );
                
                log.info("管理员批准邀请，白名单用户直接加入，invitee: {}", invitation.getInviteeId());
            } else {
                // 非白名单用户，转为待被邀请人同意状态
                invitation.setStatus(1); // 待被邀请人同意
                invitation.setAdminReviewerId(adminId);
                invitation.setAdminReviewTime(LocalDateTime.now());
                invitation.setAdminReviewNote("管理员批准，等待被邀请人同意");
                groupInvitationMapper.updateById(invitation);
                
                // 通知被邀请人
                webSocketUtil.pushGroupInvitation(
                    invitation.getInviteeId(),
                    invitation.getInviterId(),
                    inviterName,
                    invitation.getGroupId(),
                    groupName,
                    invitation.getId()
                );
                
                log.info("管理员批准邀请，发送邀请给被邀请人，invitee: {}", invitation.getInviteeId());
            }
            
            // 通知邀请人审批结果
            webSocketUtil.pushGroupInviteApprovalResult(
                invitation.getInviterId(),
                adminId,
                inviteeName,
                invitation.getGroupId(),
                groupName,
                true
            );
        } else {
            // 8. 拒绝：更新邀请状态
            invitation.setStatus(3); // 管理员拒绝
            invitation.setAdminReviewerId(adminId);
            invitation.setAdminReviewTime(LocalDateTime.now());
            invitation.setAdminReviewNote("管理员拒绝");
            groupInvitationMapper.updateById(invitation);
            
            // 通知邀请人审批结果
            webSocketUtil.pushGroupInviteApprovalResult(
                invitation.getInviterId(),
                adminId,
                inviteeName,
                invitation.getGroupId(),
                groupName,
                false
            );
            
            log.info("管理员拒绝邀请，inviter: {}, invitee: {}", invitation.getInviterId(), invitation.getInviteeId());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId) {
        // 1. 获取当前用户ID（群主）
        Long ownerId = UserContext.getCurrentUserId();
        log.info("解散群组，ownerId: {}, groupId: {}", ownerId, groupId);
        
        if (ownerId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }
        
        // 3. 验证是否是群主
        GroupMember owner = groupMemberMapper.selectByGroupIdAndUserId(groupId, ownerId);
        if (owner == null || owner.getStatus() != 1 || owner.getRole() != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主可以解散群组");
        }
        
        // 4. 获取所有群成员（用于通知）
        List<GroupMember> allMembers = groupMemberMapper.selectByGroupId(groupId);
        
        // 5. 解散群组（软删除）
        group.setStatus(0); // 设置为已解散状态
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
        
        // 6. 设置所有成员状态为已退出
        for (GroupMember member : allMembers) {
            member.setStatus(0); // 已退出
            member.setUpdateTime(LocalDateTime.now());
            groupMemberMapper.updateById(member);
        }
        
        // 7. 通知所有群成员群组已解散
        User ownerUser = userMapper.selectById(ownerId);
        String ownerName = ownerUser != null ? ownerUser.getNickname() : "群主";
        
        for (GroupMember member : allMembers) {
            webSocketUtil.pushGroupDissolveNotification(
                member.getUserId(),
                ownerId,
                ownerName,
                groupId,
                group.getGroupName()
            );
        }
        
        log.info("群组解散成功，groupId: {}, 通知成员数量: {}", groupId, allMembers.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long userId) {
        // 1. 获取当前用户ID（操作者）
        Long operatorId = UserContext.getCurrentUserId();
        log.info("移除群成员，operatorId: {}, groupId: {}, targetUserId: {}", operatorId, groupId, userId);
        
        if (operatorId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }
        
        // 3. 验证操作者权限（必须是管理员或群主）
        GroupMember operator = groupMemberMapper.selectByGroupIdAndUserId(groupId, operatorId);
        if (operator == null || operator.getStatus() != 1 || operator.getRole() < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有管理员和群主可以移除成员");
        }
        
        // 4. 查询目标用户的成员信息
        GroupMember targetMember = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (targetMember == null || targetMember.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户不是群组成员");
        }
        
        // 5. 权限检查
        // 群主不能被移除
        if (targetMember.getRole() == 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移除群主");
        }
        
        // 管理员只能移除普通成员，不能移除其他管理员
        if (operator.getRole() == 1 && targetMember.getRole() >= 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员只能移除普通成员");
        }
        
        // 不能移除自己
        if (userId.equals(operatorId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能移除自己，请使用退出群组功能");
        }
        
        // 6. 移除成员（软删除）
        targetMember.setStatus(0); // 设置为已退出状态
        targetMember.setUpdateTime(LocalDateTime.now());
        groupMemberMapper.updateById(targetMember);
        
        // 7. 获取被移除用户信息
        User removedUser = userMapper.selectById(userId);
        String removedUserName = removedUser != null ? removedUser.getNickname() : "未知用户";
        
        // 8. 通知被移除的用户
        webSocketUtil.pushGroupRemoveNotification(
            userId,
            operatorId,
            removedUserName,
            groupId,
            group.getGroupName(),
            true  // 被移除的人
        );
        
        // 9. 通知群主和其他管理员
        List<GroupMember> admins = groupMemberMapper.selectAdminsByGroupId(groupId);
        for (GroupMember admin : admins) {
            if (!admin.getUserId().equals(operatorId)) {
                webSocketUtil.pushGroupRemoveNotification(
                    admin.getUserId(),
                    operatorId,
                    removedUserName,
                    groupId,
                    group.getGroupName(),
                    false  // 管理员/群主
                );
            }
        }
        
        log.info("移除群成员成功，operatorId: {}, userId: {}, groupId: {}", operatorId, userId, groupId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO updateGroup(Long groupId, GroupDTO groupDTO) {
        // 1. 获取当前用户ID（操作者）
        Long currentUserId = UserContext.getCurrentUserId();
        log.info("修改群组信息，operatorId: {}, groupId: {}", currentUserId, groupId);
        
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 2. 查询群组是否存在
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }
        
        // 3. 验证操作者权限（只有群主可以修改群信息）
        GroupMember operator = groupMemberMapper.selectByGroupIdAndUserId(groupId, currentUserId);
        if (operator == null || operator.getStatus() != 1 || operator.getRole() != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主可以修改群组信息");
        }
        
        // 4. 记录公告是否变更（用于通知）
        String oldNotice = group.getNotice();
        boolean noticeChanged = false;
        
        // 5. 更新群组信息
        if (groupDTO.getGroupName() != null && !groupDTO.getGroupName().trim().isEmpty()) {
            group.setGroupName(groupDTO.getGroupName());
        }
        
        if (groupDTO.getAvatar() != null) {
            group.setAvatar(groupDTO.getAvatar());
        }
        
        if (groupDTO.getNotice() != null) {
            if (!groupDTO.getNotice().equals(oldNotice)) {
                noticeChanged = true;
            }
            group.setNotice(groupDTO.getNotice());
        }
        
        if (groupDTO.getMaxMembers() != null && groupDTO.getMaxMembers() > 0) {
            // 检查最大成员数是否小于当前成员数
            int currentMemberCount = groupMemberMapper.countByGroupId(groupId);
            if (groupDTO.getMaxMembers() < currentMemberCount) {
                throw new BusinessException(ResultCode.BAD_REQUEST, 
                    "最大成员数不能小于当前成员数（" + currentMemberCount + "）");
            }
            group.setMaxMembers(groupDTO.getMaxMembers());
        }
        
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
        
        log.info("群组信息修改成功，groupId: {}", groupId);
        
        // 6. 只有群公告更新时才通知所有成员
        if (noticeChanged) {
            User operatorUser = userMapper.selectById(currentUserId);
            String operatorName = operatorUser != null ? operatorUser.getNickname() : "群主";
            List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
            
            for (GroupMember member : members) {
                if (!member.getUserId().equals(currentUserId)) {
                    webSocketUtil.pushGroupInfoUpdateNotification(
                        member.getUserId(),
                        currentUserId,
                        operatorName,
                        groupId,
                        group.getGroupName(),
                        "notice"
                    );
                }
            }
        }
        
        // 7. 返回更新后的群组信息
        return getGroupById(groupId);
    }
    
    @Override
    public void setGroupMuted(Long groupId, boolean muted) {
        Long userId = UserContext.getCurrentUserId();
        log.info("设置群免打扰，userId: {}, groupId: {}, muted: {}", userId, groupId, muted);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 检查是否是群成员
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不是该群成员");
        }
        
        // 更新免打扰状态
        int updated = groupMemberMapper.updateMuted(groupId, userId, muted ? 1 : 0);
        if (updated > 0) {
            log.info("设置群免打扰成功，userId: {}, groupId: {}, muted: {}", userId, groupId, muted);
        }
    }
    
    @Override
    public boolean isGroupMuted(Long groupId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        return member != null && member.getMuted() != null && member.getMuted() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteMember(Long groupId, Long userId, LocalDateTime muteUntil) {
        Long operatorId = UserContext.getCurrentUserId();
        log.info("设置群成员禁言，operatorId: {}, groupId: {}, targetUserId: {}, muteUntil: {}", operatorId, groupId, userId, muteUntil);

        if (operatorId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "群组不存在或已解散");
        }

        GroupMember operator = groupMemberMapper.selectByGroupIdAndUserId(groupId, operatorId);
        if (operator == null || operator.getStatus() != 1 || operator.getRole() < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主或管理员可以禁言成员");
        }

        GroupMember target = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (target == null || target.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户不是群成员");
        }

        if (target.getRole() != null && target.getRole() == 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能禁言群主");
        }

        if (operator.getRole() != null && operator.getRole() == 1 && target.getRole() != null && target.getRole() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员不能禁言其他管理员");
        }

        groupMemberMapper.updateMuteUntil(groupId, userId, muteUntil);

        // 推送禁言/解除禁言通知给目标用户
        User operatorUser = userMapper.selectById(operatorId);
        String operatorName = operatorUser != null ? operatorUser.getNickname() : "管理员";
        webSocketUtil.pushGroupMuteNotification(userId, operatorId, operatorName,
                groupId, group.getGroupName(), muteUntil);
    }
}
