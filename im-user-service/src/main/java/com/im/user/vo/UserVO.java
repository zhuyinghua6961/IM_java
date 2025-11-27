package com.im.user.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String phone;
    private String email;
    private String signature;
}
