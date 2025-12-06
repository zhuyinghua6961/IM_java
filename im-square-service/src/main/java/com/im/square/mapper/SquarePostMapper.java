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

    /**
     * 按关键字和标签搜索公开帖子列表
     */
    List<SquarePost> searchPublic(@Param("keyword") String keyword,
                                  @Param("tags") java.util.List<String> tags,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 按关键字和标签搜索公开帖子总数
     */
    long countSearchPublic(@Param("keyword") String keyword,
                           @Param("tags") java.util.List<String> tags);

    /**
     * 按热度排序的公开帖子列表
     */
    List<SquarePost> selectPublicHotList(@Param("offset") int offset,
                                         @Param("limit") int limit);

    List<SquarePost> selectByUser(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId);

    List<SquarePost> selectByIds(@Param("ids") List<Long> ids);

    long sumLikesByUser(@Param("userId") Long userId);
}
