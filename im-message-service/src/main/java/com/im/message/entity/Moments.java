package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态（广场帖子）实体，对应表：moments
 */
@Data
public class Moments {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 文本内容 */
    private String content;

    /** 图片 URL 数组的 JSON 字符串 */
    private String images;

    /** 视频 URL */
    private String video;

    /** 位置 */
    private String location;

    /** 可见范围 0-公开 1-私密 2-部分可见（广场场景下默认 0） */
    private Integer visibleType;

    /** 可见用户ID数组 JSON（仅部分可见时使用） */
    private String visibleUsers;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 状态 0-已删除 1-正常 */
    private Integer status;

    private LocalDateTime createTime;
}
