package com.im.user.dto;

import lombok.Data;
import java.util.List;

/**
 * 群组DTO
 */
@Data
public class GroupDTO {
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
    
    /**
     * 初始成员ID列表（创建群组时使用）
     */
    private List<Long> memberIds;
}
