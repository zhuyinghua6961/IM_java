package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友关系实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Friend extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 好友ID
     */
    private Long friendId;
    
    /**
     * 备注名
     */
    private String remark;
    
    /**
     * 状态 0-已删除 1-正常
     */
    private Integer status;
}
