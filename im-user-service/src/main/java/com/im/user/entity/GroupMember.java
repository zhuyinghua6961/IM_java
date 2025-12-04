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
    private Integer muted; // 免打扰：0-正常，1-免打扰
    private LocalDateTime muteUntil; // 禁言截止时间，null 表示未禁言
}
