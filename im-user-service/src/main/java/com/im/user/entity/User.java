package com.im.user.entity;

import com.im.common.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码(加密)
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 个性签名
     */
    private String signature;
    
    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;
}
