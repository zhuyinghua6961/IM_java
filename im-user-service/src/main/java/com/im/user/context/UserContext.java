package com.im.user.context;

/**
 * 用户上下文 - 存储当前登录用户信息
 * 使用 ThreadLocal 保证线程安全
 */
public class UserContext {
    
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    
    /**
     * 设置当前用户ID
     */
    public static void setCurrentUserId(Long userId) {
        userIdHolder.set(userId);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return userIdHolder.get();
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clear() {
        userIdHolder.remove();
    }
}
