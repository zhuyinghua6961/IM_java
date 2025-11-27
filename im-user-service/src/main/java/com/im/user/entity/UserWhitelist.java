package com.im.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户白名单实体
 */
@Data
public class UserWhitelist {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 白名单好友ID
     */
    private Long friendId;
    
    /**
     * 添加时间
     */
    private LocalDateTime createTime;
}
