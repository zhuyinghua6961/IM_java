package com.im.common.enums;

import lombok.Getter;

/**
 * 统一结果码枚举
 */
@Getter
public enum ResultCode {
    // 成功
    SUCCESS(200, "success"),
    
    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    
    // 服务器错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 用户模块 1000-1999
    // 业务状态码 (1000-9999)
    USERNAME_EXIST(1001, "用户名已存在"),
    USER_NOT_EXIST(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    PHONE_EXIST(1005, "手机号已被注册"),
    USER_DISABLED(1006, "账号已被禁用"),
    
    // 好友模块 2000-2999
    FRIEND_EXIST(2001, "好友已存在"),
    FRIEND_REQUEST_SENT(2002, "好友申请已发送"),
    FRIEND_REQUEST_NOT_EXIST(2003, "好友申请不存在"),
    CANNOT_ADD_SELF(2004, "不能添加自己为好友"),
    BLOCKED_BY_USER(2005, "对方已拉黑你"),
    
    // 消息模块 3000-3999
    MESSAGE_SEND_FAIL(3001, "消息发送失败"),
    MESSAGE_NOT_FOUND(3002, "消息不存在"),
    MESSAGE_RECALLED(3003, "消息已撤回"),
    RECALL_TIMEOUT(3004, "撤回时间已过"),
    NOT_MESSAGE_SENDER(3005, "不是消息发送者"),
    
    // 群组模块 4000-4999
    GROUP_NOT_FOUND(4001, "群组不存在"),
    NOT_GROUP_MEMBER(4002, "不是群成员"),
    NO_PERMISSION(4003, "没有权限"),
    GROUP_FULL(4004, "群组已满"),
    ALREADY_GROUP_MEMBER(4005, "已经是群成员"),
    
    // 朋友圈模块 5000-5999
    MOMENTS_NOT_FOUND(5001, "动态不存在"),
    NO_VIEW_PERMISSION(5002, "没有查看权限"),
    ALREADY_LIKED(5003, "已经点赞过"),
    COMMENT_NOT_FOUND(5004, "评论不存在"),
    
    // 文件模块 6000-6999
    FILE_UPLOAD_FAIL(6001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORT(6002, "文件类型不支持"),
    FILE_SIZE_EXCEED(6003, "文件大小超限"),
    FILE_NOT_FOUND(6004, "文件不存在");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
