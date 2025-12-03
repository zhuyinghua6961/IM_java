package com.im.message.mapper;

import com.im.message.entity.GroupAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群公告 Mapper
 */
@Mapper
public interface GroupAnnouncementMapper {
    
    /**
     * 插入群公告
     */
    int insert(GroupAnnouncement announcement);
    
    /**
     * 更新群公告
     */
    int update(GroupAnnouncement announcement);
    
    /**
     * 删除群公告
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据ID查询
     */
    GroupAnnouncement selectById(@Param("id") Long id);
    
    /**
     * 查询群的所有公告（按置顶和时间排序）
     */
    List<GroupAnnouncement> selectByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 查询群的最新公告
     */
    GroupAnnouncement selectLatestByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 取消群内其他公告的置顶
     */
    int cancelTopByGroupId(@Param("groupId") Long groupId, @Param("excludeId") Long excludeId);
}
