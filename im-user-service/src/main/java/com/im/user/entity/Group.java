package com.im.user.entity;

import com.im.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Group extends BaseEntity {
    private String groupName;
    private String avatar;
    private Long ownerId;
    private String notice;
    private Integer maxMembers;
    private Integer status;
}
