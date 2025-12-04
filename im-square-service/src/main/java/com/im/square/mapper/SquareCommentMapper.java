package com.im.square.mapper;

import com.im.square.entity.SquareComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SquareCommentMapper {

    int insert(SquareComment comment);

    SquareComment selectById(@Param("id") Long id);

    int softDelete(@Param("id") Long id,
                   @Param("userId") Long userId);

    List<SquareComment> selectByPost(@Param("postId") Long postId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long countByPost(@Param("postId") Long postId);
}
