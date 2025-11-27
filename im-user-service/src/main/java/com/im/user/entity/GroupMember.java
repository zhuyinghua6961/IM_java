package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class GroupMember extends BaseEntity {
    private Long groupId;
    private Long userId;
    private Integer role;
    private String nickname;
    private LocalDateTime joinTime;
    private Integer status;
}
