package com.im.message.service;

import com.im.message.entity.GroupAnnouncement;

import java.util.List;

/**
 * 群公告 Service
 */
public interface GroupAnnouncementService {
    
    /**
     * 发布群公告
     * @param userId 发布者ID
     * @param groupId 群ID
     * @param title 标题
     * @param content 内容
     * @param isTop 是否置顶
     * @return 公告ID
     */
    Long publish(Long userId, Long groupId, String title, String content, boolean isTop);
    
    /**
     * 更新群公告
     * @param userId 操作者ID
     * @param announcementId 公告ID
     * @param title 标题
     * @param content 内容
     * @param isTop 是否置顶
     */
    void update(Long userId, Long announcementId, String title, String content, boolean isTop);
    
    /**
     * 删除群公告
     * @param userId 操作者ID
     * @param announcementId 公告ID
     */
    void delete(Long userId, Long announcementId);
    
    /**
     * 获取群公告列表
     * @param groupId 群ID
     * @return 公告列表
     */
    List<GroupAnnouncement> getList(Long groupId);
    
    /**
     * 获取群最新公告
     * @param groupId 群ID
     * @return 最新公告
     */
    GroupAnnouncement getLatest(Long groupId);
    
    /**
     * 获取公告详情
     * @param announcementId 公告ID
     * @return 公告详情
     */
    GroupAnnouncement getById(Long announcementId);
}
