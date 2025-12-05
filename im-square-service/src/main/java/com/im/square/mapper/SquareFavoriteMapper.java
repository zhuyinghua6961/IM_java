package com.im.square.mapper;

import com.im.square.entity.SquareFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SquareFavoriteMapper {

    int insert(SquareFavorite favorite);

    int delete(@Param("postId") Long postId,
               @Param("userId") Long userId);

    int countByPostAndUser(@Param("postId") Long postId,
                           @Param("userId") Long userId);

    int countByPost(@Param("postId") Long postId);

    /**
     * 查询某个用户收藏的帖子ID列表，按收藏时间倒序
     */
    List<Long> selectPostIdsByUser(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计某个用户收藏的帖子数量
     */
    long countByUser(@Param("userId") Long userId);
}
