package com.im.user.service;

import com.im.user.dto.LoginDTO;
import com.im.user.dto.UserDTO;
import com.im.user.entity.User;
import com.im.user.vo.LoginVO;
import com.im.user.vo.UserVO;

public interface UserService {
    UserVO register(UserDTO userDTO);
    LoginVO login(LoginDTO loginDTO);
    void logout(Long userId);
    UserVO getUserById(Long id);
    void updateUser(Long userId, UserDTO userDTO);
    UserVO searchUser(String keyword);
}
