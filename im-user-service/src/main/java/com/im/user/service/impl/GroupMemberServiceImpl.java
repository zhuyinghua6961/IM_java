package com.im.user.service.impl;

import com.im.user.entity.GroupMember;
import com.im.user.mapper.GroupMemberMapper;
import com.im.user.service.GroupMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群成员服务实现
 */
@Slf4j
@Service
public class GroupMemberServiceImpl implements GroupMemberService {
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        log.info("查询群成员列表，groupId: {}", groupId);
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        log.info("查询成功，成员数量: {}", members.size());
        return members;
    }
    
    @Override
    public GroupMember getGroupMember(Long groupId, Long userId) {
        log.debug("查询群成员信息，groupId: {}, userId: {}", groupId, userId);
        return groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
    }
    
    @Override
    public boolean isMember(Long groupId, Long userId) {
        GroupMember member = getGroupMember(groupId, userId);
        // 成员存在且状态为正常（status=1）
        boolean isMember = member != null && member.getStatus() != null && member.getStatus() == 1;
        log.debug("检查群成员身份，groupId: {}, userId: {}, isMember: {}", groupId, userId, isMember);
        return isMember;
    }
    
    @Override
    public List<Long> getGroupMemberIds(Long groupId, Long excludeUserId) {
        log.debug("获取群成员ID列表，groupId: {}, excludeUserId: {}", groupId, excludeUserId);
        List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
        
        List<Long> memberIds = members.stream()
            .filter(m -> m.getStatus() != null && m.getStatus() == 1) // 只要正常状态的成员
            .filter(m -> !m.getUserId().equals(excludeUserId)) // 排除指定用户
            .map(GroupMember::getUserId)
            .collect(Collectors.toList());
        
        log.debug("获取成功，成员数量: {}", memberIds.size());
        return memberIds;
    }
}
