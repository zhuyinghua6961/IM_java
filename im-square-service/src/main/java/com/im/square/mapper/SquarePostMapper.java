package com.im.square.mapper;

import com.im.square.entity.SquarePost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SquarePostMapper {

    int insert(SquarePost post);

    SquarePost selectById(@Param("id") Long id);

    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    int updatePost(SquarePost post);

    int updateCounters(@Param("id") Long id,
                       @Param("likeDelta") int likeDelta,
                       @Param("commentDelta") int commentDelta);

    List<SquarePost> selectPublicList(@Param("offset") int offset,
                                      @Param("limit") int limit);

    long countPublic();

    List<SquarePost> selectByUser(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId);

    List<SquarePost> selectByIds(@Param("ids") List<Long> ids);

    long sumLikesByUser(@Param("userId") Long userId);
}
