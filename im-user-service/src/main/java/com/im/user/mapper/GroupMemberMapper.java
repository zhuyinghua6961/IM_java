package com.im.user.mapper;

import com.im.user.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群成员Mapper
 */
@Mapper
public interface GroupMemberMapper {
    /**
     * 插入群成员
     */
    int insert(GroupMember groupMember);
    
    /**
     * 批量插入群成员
     */
    int batchInsert(@Param("members") List<GroupMember> members);
    
    /**
     * 根据ID查询
     */
    GroupMember selectById(@Param("id") Long id);
    
    /**
     * 查询群成员列表
     */
    List<GroupMember> selectByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 查询用户在某个群的成员信息（只查询status=1的）
     */
    GroupMember selectByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 查询用户在某个群的成员信息（不限制status，包括已退出的）
     */
    GroupMember selectByGroupIdAndUserIdIncludeInactive(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 统计群成员数量
     */
    int countByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 删除群成员
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 更新成员角色
     */
    int updateRole(@Param("id") Long id, @Param("role") Integer role);
    
    /**
     * 更新群成员信息
     */
    int updateById(GroupMember groupMember);
    
    /**
     * 查询群组的管理员和群主（用于通知）
     */
    List<GroupMember> selectAdminsByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 更新免打扰状态
     */
    int updateMuted(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("muted") Integer muted);

    /**
     * 更新禁言截止时间
     */
    int updateMuteUntil(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("muteUntil") java.time.LocalDateTime muteUntil);
}
