package com.im.user.vo;

import lombok.Data;

/**
 * 好友信息VO
 */
@Data
public class FriendVO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;
    
    /**
     * 个性签名
     */
    private String signature;
    
    /**
     * 备注名
     */
    private String remark;
    
    /**
     * 是否在线（暂未实现）
     */
    private Boolean online = false;
}
