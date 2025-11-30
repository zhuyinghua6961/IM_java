package com.im.user.controller;

import com.im.common.enums.ResultCode;
import com.im.common.vo.Result;
import com.im.user.context.UserContext;
import com.im.user.dto.LoginDTO;
import com.im.user.dto.UserDTO;
import com.im.user.service.UserService;
import com.im.user.vo.LoginVO;
import com.im.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private com.im.user.service.SmsCodeService smsCodeService;
    
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody UserDTO userDTO) {
        UserVO userVO = userService.register(userDTO);
        return Result.success(userVO);
    }
    
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 从 UserContext 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        userService.logout(userId);
        return Result.success();
    }
    
    @GetMapping("/info/{userId}")
    public Result<UserVO> getUserInfo(@PathVariable("userId") Long userId) {
        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody UserDTO userDTO) {
        // 从 UserContext 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        userService.updateUser(userId, userDTO);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<UserVO>> searchUser(@RequestParam("keyword") String keyword) {
        UserVO res = userService.searchUser(keyword);
        // 将单个结果包装成列表返回，保持与前端一致
        List<UserVO> list = res != null ? List.of(res) : new ArrayList<>();
        return Result.success(list);
    }
    
    /**
     * 发送短信验证码
     */
    @PostMapping("/sms/send")
    public Result<Void> sendSmsCode(@RequestBody java.util.Map<String, String> request) {
        String phone = request.get("phone");
        
        if (phone == null || phone.isEmpty()) {
            return Result.error(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        
        log.info("发送验证码: phone={}", phone);
        
        boolean success = smsCodeService.sendCode(phone);
        if (success) {
            return Result.success();
        } else {
            return Result.error(ResultCode.BAD_REQUEST, "发送验证码失败，请稍后再试");
        }
    }

}
