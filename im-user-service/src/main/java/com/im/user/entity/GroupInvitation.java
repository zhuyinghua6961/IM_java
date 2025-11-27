package com.im.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组邀请实体
 */
@Data
public class GroupInvitation {
    
    private Long id;
    
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 邀请人ID
     */
    private Long inviterId;
    
    /**
     * 被邀请人ID
     */
    private Long inviteeId;
    
    /**
     * 邀请人角色：0=普通成员 1=管理员 2=群主
     */
    private Integer inviterRole;
    
    /**
     * 状态：0=待管理员审批 1=待被邀请人同意 2=已同意 3=管理员拒绝 4=被邀请人拒绝 5=已过期
     */
    private Integer status;
    
    /**
     * 审批的管理员ID（预留）
     */
    private Long adminReviewerId;
    
    /**
     * 审批时间（预留）
     */
    private LocalDateTime adminReviewTime;
    
    /**
     * 审批备注（预留）
     */
    private String adminReviewNote;
    
    /**
     * 被邀请人回复时间
     */
    private LocalDateTime inviteeReplyTime;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
