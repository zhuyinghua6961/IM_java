package com.im.user.dto;

import lombok.Data;

/**
 * 更新群信息DTO
 */
@Data
public class GroupUpdateDTO {
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 群名称
     */
    private String groupName;
    
    /**
     * 群头像
     */
    private String avatar;
    
    /**
     * 群公告
     */
    private String notice;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
}
