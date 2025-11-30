package com.im.user.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private String signature;
    
    /**
     * 短信验证码（注册时使用）
     */
    private String code;
}
