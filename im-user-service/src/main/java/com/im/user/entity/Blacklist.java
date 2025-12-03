package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 黑名单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Blacklist extends BaseEntity {
    
    /**
     * 用户ID（拉黑的人）
     */
    private Long userId;
    
    /**
     * 被拉黑的用户ID
     */
    private Long blockedUserId;
    
    /**
     * 状态 0-已解除 1-拉黑中
     */
    private Integer status;
}
