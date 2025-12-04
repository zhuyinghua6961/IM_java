package com.im.square.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场帖子实体，对应表 square_post
 */
@Data
public class SquarePost {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String title;

    private String content;

    /** 图片 URL 数组的 JSON 字符串 */
    private String images;

    private String video;

    /** 标签 JSON 数组 */
    private String tags;

    /** 可见范围 0-公开 1-仅好友 */
    private Integer visibleType;

    /** 排除可见的好友ID JSON 数组 */
    private String excludeUsers;

    /** 状态 0-已删除 1-正常 2-审核未通过 */
    private Integer status;

    /** 审核状态 0-未审核/跳过 1-通过 2-拒绝 */
    private Integer auditStatus;

    private String auditReason;

    private Integer likeCount;

    private Integer commentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
