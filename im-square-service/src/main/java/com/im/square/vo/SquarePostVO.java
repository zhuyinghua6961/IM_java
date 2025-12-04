package com.im.square.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广场帖子返回对象
 */
@Data
public class SquarePostVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String nickname;

    private String avatar;

    private String title;

    private String content;

    /** 图片 URL 列表 */
    private List<String> images;

    /** 视频 URL */
    private String video;

    /** 标签 */
    private List<String> tags;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    private LocalDateTime createTime;
}
