package com.im.common.vo;

import com.im.common.enums.ResultCode;
import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 提示信息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // ==================== 成功响应 ====================
    
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), 
                           ResultCode.SUCCESS.getMessage(), null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), 
                           ResultCode.SUCCESS.getMessage(), data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }
    
    // ==================== 失败响应 ====================
    
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), 
                           ResultCode.INTERNAL_ERROR.getMessage(), null);
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.INTERNAL_ERROR.getCode(), message, null);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
    
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
    
    // ==================== 判断方法 ====================
    
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
    
    public boolean isError() {
        return !isSuccess();
    }
}
