package com.im.user.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.common.utils.JwtUtil;
import com.im.user.dto.LoginDTO;
import com.im.user.dto.UserDTO;
import com.im.user.entity.User;
import com.im.user.mapper.UserMapper;
import com.im.user.service.SmsCodeService;
import com.im.user.service.UserService;
import com.im.user.vo.LoginVO;
import com.im.user.vo.UserVO;
import com.im.user.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private SmsCodeService smsCodeService;
    
    private static final String DEFAULT_AVATAR = "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png";
    
    @Override
    public UserVO register(UserDTO userDTO) {
        log.info("用户注册，username: {}, phone: {}", userDTO.getUsername(), userDTO.getPhone());
        
        // 1. 校验必填字段
        if (userDTO.getPhone() == null || userDTO.getPhone().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        if (userDTO.getCode() == null || userDTO.getCode().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码不能为空");
        }
        
        // 2. 验证手机验证码
        if (!smsCodeService.verifyCode(userDTO.getPhone(), userDTO.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        
        // 3. 校验用户名是否已存在
        User existUser = userMapper.selectByUsername(userDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

        // 4. 校验手机号是否已存在
        User phoneUser = userMapper.selectByPhone(userDTO.getPhone());
        if (phoneUser != null) {
            throw new BusinessException(ResultCode.PHONE_EXIST);
        }
        
        // 5. 创建用户
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(encryptPassword(userDTO.getPassword()));
        user.setNickname(userDTO.getNickname());
        user.setPhone(userDTO.getPhone());
        user.setEmail(userDTO.getEmail());
        user.setAvatar(DEFAULT_AVATAR);
        user.setGender(0);
        user.setStatus(1);
        
        userMapper.insert(user);
        log.info("用户注册成功，userId: {}", user.getId());
        
        // 6. 清除验证码
        smsCodeService.clearCode(userDTO.getPhone());
        
        // 7. 返回用户信息
        return convertToVO(user);
    }
    
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        log.info("用户登录，loginType: {}, username: {}, phone: {}", 
            loginDTO.getLoginType(), loginDTO.getUsername(), loginDTO.getPhone());
        
        User user = null;
        
        // 1. 根据登录方式验证
        if ("sms".equals(loginDTO.getLoginType())) {
            // 验证码登录
            user = loginBySms(loginDTO);
        } else {
            // 密码登录（默认）
            user = loginByPassword(loginDTO);
        }
        
        // 2. 检查账号状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        // 3. 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 4. 将Token存入Redis，设置7天过期
        String redisKey = RedisConstant.getUserTokenKey(user.getId());
        redisTemplate.opsForValue().set(redisKey, token, RedisConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
        log.info("Token已缓存到Redis，key: {}", redisKey);
        
        // 5. 构造返回对象
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(convertToVO(user));
        
        log.info("用户登录成功，userId: {}", user.getId());
        return loginVO;
    }
    
    /**
     * 验证码登录
     */
    private User loginBySms(LoginDTO loginDTO) {
        String phone = loginDTO.getPhone();
        String code = loginDTO.getCode();
        
        if (phone == null || phone.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "手机号不能为空");
        }
        if (code == null || code.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码不能为空");
        }
        
        // 1. 验证验证码
        if (!smsCodeService.verifyCode(phone, code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
        
        // 2. 查询用户
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "该手机号未注册");
        }
        
        // 3. 清除验证码
        smsCodeService.clearCode(phone);
        
        return user;
    }
    
    /**
     * 密码登录（支持用户名或手机号）
     */
    private User loginByPassword(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String phone = loginDTO.getPhone();
        String password = loginDTO.getPassword();
        
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码不能为空");
        }
        
        User user = null;
        
        // 1. 查询用户（优先手机号，其次用户名）
        if (phone != null && !phone.isEmpty()) {
            user = userMapper.selectByPhone(phone);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST, "该手机号未注册");
            }
        } else if (username != null && !username.isEmpty()) {
            user = userMapper.selectByUsername(username);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
            }
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名和手机号不能同时为空");
        }
        
        // 2. 验证密码
        String encryptedPassword = encryptPassword(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        
        return user;
    }
    
    @Override
    public void logout(Long userId) {
        log.info("用户登出，userId: {}", userId);
        
        // 从Redis中删除Token
        String redisKey = RedisConstant.getUserTokenKey(userId);
        redisTemplate.delete(redisKey);
        
        log.info("用户登出成功，已清除Token");
    }
    
    @Override
    public UserVO getUserById(Long id) {
        log.info("查询用户信息，userId: {}", id);
        
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        return convertToVO(user);
    }
    
    @Override
    public void updateUser(Long userId, UserDTO userDTO) {
        log.info("更新用户信息，userId: {}", userId);
        
        // 1. 校验用户是否存在
        User existUser = userMapper.selectById(userId);
        if (existUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 2. 设置用户ID并更新
        userDTO.setId(userId);
        userMapper.update(userDTO);
        log.info("用户信息更新成功");
    }
    
    /**
     * 密码加密
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 转换为VO对象
     */
    private UserVO convertToVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setUserId(user.getId());
        return userVO;
    }


    @Override
    public UserVO searchUser(String keyword) {
        User user = userMapper.selectByUsername(keyword);
        if (user == null) {
            user = userMapper.selectByPhone(keyword);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_EXIST);
            }
        }
        return convertToVO(user);
    }

}
