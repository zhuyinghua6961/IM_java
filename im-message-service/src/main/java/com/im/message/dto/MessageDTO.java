package com.im.message.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private Long toUserId;      // 接收用户ID（单聊）
    private Long groupId;       // 群组ID（群聊）
    private Integer chatType;   // 聊天类型 1-单聊 2-群聊
    private Integer msgType;    // 消息类型 1-文本 2-图片 3-视频 4-文件 5-语音
    private String content;     // 消息内容
    private String url;         // 媒体文件URL
    private String atUserIds;   // 被@的用户ID列表，逗号分隔，"all"表示@全体成员
}
