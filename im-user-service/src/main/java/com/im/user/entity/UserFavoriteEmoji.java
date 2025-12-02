package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收藏表情实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserFavoriteEmoji extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 表情图片的 OSS 访问地址
     */
    private String url;

    /**
     * 文件名或自定义名称
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 图片宽度（像素）
     */
    private Integer width;

    /**
     * 图片高度（像素）
     */
    private Integer height;

    /**
     * 内容类型（MIME），例如 image/png
     */
    private String contentType;

    /**
     * 状态 1-正常 0-删除
     */
    private Integer status;
}
