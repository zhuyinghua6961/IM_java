package com.im.user.dto;

import lombok.Data;

/**
 * 更新好友备注请求 DTO
 */
@Data
public class FriendRemarkDTO {

    /**
     * 好友ID
     */
    private Long friendId;

    /**
     * 备注名
     */
    private String remark;
}
