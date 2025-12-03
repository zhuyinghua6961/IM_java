package com.im.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 黑名单VO
 */
@Data
public class BlacklistVO {
    
    /**
     * 黑名单记录ID
     */
    private Long id;
    
    /**
     * 被拉黑的用户ID
     */
    private Long blockedUserId;
    
    /**
     * 被拉黑用户的用户名
     */
    private String username;
    
    /**
     * 被拉黑用户的昵称
     */
    private String nickname;
    
    /**
     * 被拉黑用户的头像
     */
    private String avatar;
    
    /**
     * 拉黑时间
     */
    private LocalDateTime createTime;
}
