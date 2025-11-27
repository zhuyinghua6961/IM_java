package com.im.user.mapper;

import com.im.user.entity.GroupInvitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群组邀请 Mapper
 */
@Mapper
public interface GroupInvitationMapper {
    
    /**
     * 根据ID查询
     */
    GroupInvitation selectById(@Param("id") Long id);
    
    /**
     * 更新
     */
    int updateById(GroupInvitation invitation);
    
    /**
     * 查询是否存在待处理的邀请
     */
    GroupInvitation selectPendingInvitation(@Param("groupId") Long groupId, 
                                           @Param("inviteeId") Long inviteeId);
    
    /**
     * 批量插入邀请记录
     */
    int batchInsert(@Param("list") List<GroupInvitation> list);
    
    /**
     * 查询用户收到的待处理邀请列表
     */
    List<GroupInvitation> selectPendingByInviteeId(@Param("inviteeId") Long inviteeId);
}
