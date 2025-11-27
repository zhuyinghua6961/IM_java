package com.im.user.mapper;

import com.im.user.entity.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GroupMapper {
    int insert(Group group);
    Group selectById(@Param("id") Long id);
    List<Group> selectByUserId(@Param("userId") Long userId);
    int update(Group group);
    int updateById(Group group);
    int deleteById(@Param("id") Long id);
}
