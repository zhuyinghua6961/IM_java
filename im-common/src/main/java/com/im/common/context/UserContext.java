package com.im.common.context;

/**
 * 用户上下文 - 存储当前请求的用户信息
 */
public class UserContext {
    
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    
    /**
     * 设置当前用户ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID.set(userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return USER_ID.get();
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_ID.remove();
    }
}
