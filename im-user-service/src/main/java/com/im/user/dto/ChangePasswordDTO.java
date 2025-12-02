package com.im.user.dto;

import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 短信验证码
     */
    private String code;
}
