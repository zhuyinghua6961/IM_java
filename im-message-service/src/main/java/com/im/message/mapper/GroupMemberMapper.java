package com.im.message.mapper;

import com.im.message.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群成员Mapper（用于消息服务）
 */
@Mapper
public interface GroupMemberMapper {
    
    /**
     * 查询用户在某个群的成员信息
     */
    GroupMember selectByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 查询群成员列表
     */
    List<GroupMember> selectByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 查询用户所在的所有群组
     */
    List<GroupMember> selectByUserId(@Param("userId") Long userId);
}
