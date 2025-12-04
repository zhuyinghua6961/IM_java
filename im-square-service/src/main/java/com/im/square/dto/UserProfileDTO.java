package com.im.square.dto;

import lombok.Data;

/**
 * 用户基础信息（广场模块内部使用）
 */
@Data
public class UserProfileDTO {

    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;
}
