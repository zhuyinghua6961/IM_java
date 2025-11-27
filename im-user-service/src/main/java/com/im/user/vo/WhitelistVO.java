package com.im.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 白名单好友VO
 */
@Data
public class WhitelistVO {
    /**
     * 好友用户ID
     */
    private Long userId;
    
    /**
     * 好友昵称
     */
    private String nickname;
    
    /**
     * 好友头像
     */
    private String avatar;
    
    /**
     * 添加到白名单的时间
     */
    private LocalDateTime addTime;
}
