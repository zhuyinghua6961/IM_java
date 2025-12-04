package com.im.square.mapper;

import com.im.square.entity.SquareLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SquareLikeMapper {

    int insert(SquareLike like);

    int delete(@Param("postId") Long postId,
               @Param("userId") Long userId);

    int countByPostAndUser(@Param("postId") Long postId,
                            @Param("userId") Long userId);

    int countByPost(@Param("postId") Long postId);
}
