package com.im.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组VO
 */
@Data
public class GroupVO {
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
     * 群主ID
     */
    private Long ownerId;
    
    /**
     * 群公告
     */
    private String notice;
    
    /**
     * 成员数量
     */
    private Integer memberCount;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 状态 0-已解散 1-正常
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
