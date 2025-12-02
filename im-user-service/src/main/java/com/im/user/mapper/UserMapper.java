package com.im.user.mapper;

import com.im.user.dto.UserDTO;
import com.im.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User user);
    User selectById(@Param("id") Long id);
    User selectByUsername(@Param("username") String username);
    User selectByPhone(@Param("phone") String phone);
    int update(UserDTO user);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int deleteById(@Param("id") Long id);
    List<User> selectList(@Param("keyword") String keyword);
}
