package com.im.user.vo;

import lombok.Data;

/**
 * 收藏表情返回 VO
 */
@Data
public class EmojiVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 表情图片的 OSS 地址
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
}
