package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FriendRequest extends BaseEntity {
    private Long fromUserId;
    private Long toUserId;
    private String message;
    /**
     * 状态：0-待处理 1-已同意 2-已拒绝
     */
    private Integer status;
}
