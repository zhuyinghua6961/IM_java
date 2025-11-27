package com.im.user.dto;

import lombok.Data;

/**
 * 好友申请DTO
 */
@Data
public class FriendRequestDTO {
    /**
     * 好友ID（对方用户ID）- 发送申请时使用
     */
    private Long friendId;
    
    /**
     * 申请消息 - 发送申请时使用
     */
    private String message;
    
    /**
     * 申请ID - 处理申请时使用
     */
    private Long requestId;
    
    /**
     * 处理状态 - 处理申请时使用
     * 1-同意 2-拒绝
     */
    private Integer status;
}
