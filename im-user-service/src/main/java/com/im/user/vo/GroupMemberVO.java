package com.im.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群成员VO
 */
@Data
public class GroupMemberVO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 群内昵称
     */
    private String groupNickname;
    
    /**
     * 角色 0-普通成员 1-管理员 2-群主
     */
    private Integer role;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}
