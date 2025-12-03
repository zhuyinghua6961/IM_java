package com.im.message.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.message.entity.GroupAnnouncement;
import com.im.message.entity.GroupMember;
import com.im.message.mapper.GroupAnnouncementMapper;
import com.im.message.mapper.GroupMemberMapper;
import com.im.message.service.GroupAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群公告 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupAnnouncementServiceImpl implements GroupAnnouncementService {
    
    private final GroupAnnouncementMapper announcementMapper;
    private final GroupMemberMapper groupMemberMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, Long groupId, String title, String content, boolean isTop) {
        log.info("发布群公告: userId={}, groupId={}, title={}", userId, groupId, title);
        
        // 检查权限（只有群主和管理员可以发布公告）
        checkAdminPermission(userId, groupId);
        
        // 创建公告
        GroupAnnouncement announcement = new GroupAnnouncement();
        announcement.setGroupId(groupId);
        announcement.setPublisherId(userId);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsTop(isTop ? 1 : 0);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        
        announcementMapper.insert(announcement);
        
        log.info("群公告发布成功: id={}", announcement.getId());
        return announcement.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long announcementId, String title, String content, boolean isTop) {
        log.info("更新群公告: userId={}, announcementId={}", userId, announcementId);
        
        // 获取公告
        GroupAnnouncement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        
        // 检查权限
        checkAdminPermission(userId, announcement.getGroupId());
        
        // 更新公告
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsTop(isTop ? 1 : 0);
        announcement.setUpdateTime(LocalDateTime.now());
        
        announcementMapper.update(announcement);
        
        log.info("群公告更新成功: id={}", announcementId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long announcementId) {
        log.info("删除群公告: userId={}, announcementId={}", userId, announcementId);
        
        // 获取公告
        GroupAnnouncement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        
        // 检查权限
        checkAdminPermission(userId, announcement.getGroupId());
        
        // 删除公告
        announcementMapper.deleteById(announcementId);
        
        log.info("群公告删除成功: id={}", announcementId);
    }
    
    @Override
    public List<GroupAnnouncement> getList(Long groupId) {
        return announcementMapper.selectByGroupId(groupId);
    }
    
    @Override
    public GroupAnnouncement getLatest(Long groupId) {
        return announcementMapper.selectLatestByGroupId(groupId);
    }
    
    @Override
    public GroupAnnouncement getById(Long announcementId) {
        return announcementMapper.selectById(announcementId);
    }
    
    /**
     * 检查用户是否有管理员权限（群主或管理员）
     */
    private void checkAdminPermission(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null || member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该群成员");
        }
        Integer role = member.getRole();
        if (role == null || (role != 2 && role != 1)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有群主或管理员可以操作公告");
        }
    }
}
