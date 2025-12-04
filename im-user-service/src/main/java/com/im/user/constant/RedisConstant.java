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
     * 好友列表 Key 前缀
     */
    public static final String FRIEND_LIST_PREFIX = "user:friend:list:";

    /**
     * 群组列表 Key 前缀
     */
    public static final String GROUP_LIST_PREFIX = "user:group:list:";

    /**
     * 好友/群组列表缓存过期时间（分钟）
     */
    public static final long FRIEND_GROUP_LIST_EXPIRE_MINUTES = 30L;

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

    /**
     * 获取好友列表缓存 Key
     */
    public static String getFriendListKey(Long userId) {
        return FRIEND_LIST_PREFIX + userId;
    }

    /**
     * 获取群组列表缓存 Key
     */
    public static String getGroupListKey(Long userId) {
        return GROUP_LIST_PREFIX + userId;
    }
}
