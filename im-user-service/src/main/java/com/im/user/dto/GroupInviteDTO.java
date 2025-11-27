package com.im.user.dto;

import lombok.Data;
import java.util.List;

/**
 * 邀请入群DTO
 */
@Data
public class GroupInviteDTO {
    /**
     * 群组ID
     */
    private Long groupId;
    
    /**
     * 被邀请的用户ID列表
     */
    private List<Long> userIds;
}
