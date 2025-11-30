package com.im.user.constant;

/**
 * Redis 常量
 */
public class RedisConstant {
    
    /**
     * 用户Token Key前缀
     */
    public static final String USER_TOKEN_PREFIX = "user:token:";
    
    /**
     * Token过期时间（秒）- 7天
     */
    public static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;
    
    /**
     * 短信验证码Key前缀
     */
    public static final String SMS_CODE_PREFIX = "sms:code:";
    
    /**
     * 获取用户Token的Redis Key
     */
    public static String getUserTokenKey(Long userId) {
        return USER_TOKEN_PREFIX + userId;
    }
    
    /**
     * 获取短信验证码的Redis Key
     */
    public static String getSmsCodeKey(String phone) {
        return SMS_CODE_PREFIX + phone;
    }
}
