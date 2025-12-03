package com.im.message.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群公告实体
 */
@Data
public class GroupAnnouncement {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long publisherId;
    
    private String title;
    
    private String content;
    
    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    // 非数据库字段，用于返回发布者信息
    private transient String publisherName;
    private transient String publisherAvatar;
}
