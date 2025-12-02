package com.im.user.mapper;

import com.im.user.entity.UserFavoriteEmoji;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFavoriteEmojiMapper {

    int insert(UserFavoriteEmoji emoji);

    List<UserFavoriteEmoji> selectByUserId(@Param("userId") Long userId);

    UserFavoriteEmoji selectByUserIdAndUrl(@Param("userId") Long userId, @Param("url") String url);

    UserFavoriteEmoji selectById(@Param("id") Long id);

    int updateStatusByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("status") Integer status);
}
