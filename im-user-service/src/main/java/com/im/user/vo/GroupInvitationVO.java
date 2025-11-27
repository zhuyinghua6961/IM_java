package com.im.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组邀请VO
 */
@Data
public class GroupInvitationVO {
    
    /**
     * 邀请ID
     */
    private Long id;
    
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 群组名称
     */
    private String groupName;
    
    /**
     * 群组头像
     */
    private String groupAvatar;
    
    /**
     * 邀请人ID
     */
    private Long inviterId;
    
    /**
     * 邀请人昵称
     */
    private String inviterNickname;
    
    /**
     * 邀请人头像
     */
    private String inviterAvatar;
    
    /**
     * 状态：0=待管理员审批 1=待被邀请人同意 2=已同意 3=管理员拒绝 4=被邀请人拒绝 5=已过期
     */
    private Integer status;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
