package com.im.user.dto;

import lombok.Data;

@Data
public class LoginDTO {
    /**
     * 登录方式：password-密码登录, sms-验证码登录
     */
    private String loginType;
    
    /**
     * 用户名（密码登录时使用）
     */
    private String username;
    
    /**
     * 手机号（验证码登录或手机号密码登录时使用）
     */
    private String phone;
    
    /**
     * 密码（密码登录时使用）
     */
    private String password;
    
    /**
     * 验证码（验证码登录时使用）
     */
    private String code;
}
