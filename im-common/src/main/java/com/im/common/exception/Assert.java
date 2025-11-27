package com.im.common.exception;

import com.im.common.enums.ResultCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * 断言工具类
 */
public class Assert {
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object obj, ResultCode resultCode) {
        if (obj == null) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言对象为空
     */
    public static void isNull(Object obj, ResultCode resultCode) {
        if (obj != null) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void isNull(Object obj, String message) {
        if (obj != null) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言字符串不为空
     */
    public static void notEmpty(String str, ResultCode resultCode) {
        if (StringUtils.isEmpty(str)) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void notEmpty(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言集合不为空
     */
    public static void notEmpty(Collection<?> collection, ResultCode resultCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ResultCode resultCode) {
        if (!condition) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言两个对象相等
     */
    public static void equals(Object obj1, Object obj2, ResultCode resultCode) {
        if (!Objects.equals(obj1, obj2)) {
            throw new BusinessException(resultCode);
        }
    }
    
    public static void equals(Object obj1, Object obj2, String message) {
        if (!Objects.equals(obj1, obj2)) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 直接抛出异常
     */
    public static void fail(ResultCode resultCode) {
        throw new BusinessException(resultCode);
    }
    
    public static void fail(String message) {
        throw new BusinessException(message);
    }
}
