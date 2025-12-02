package com.im.user.service;

import com.im.user.dto.LoginDTO;
import com.im.user.dto.UserDTO;
import com.im.user.dto.ChangePasswordDTO;
import com.im.user.vo.LoginVO;
import com.im.user.vo.UserVO;

public interface UserService {
    /**
     * 用户注册（需要手机验证码）
     */
    UserVO register(UserDTO userDTO);
    
    /**
     * 用户登录（支持多种方式）
     */
    LoginVO login(LoginDTO loginDTO);
    
    /**
     * 用户登出
     */
    void logout(Long userId);
    
    /**
     * 获取用户信息
     */
    UserVO getUserById(Long id);
    
    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UserDTO userDTO);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO dto);
    
    /**
     * 搜索用户
     */
    UserVO searchUser(String keyword);
}
