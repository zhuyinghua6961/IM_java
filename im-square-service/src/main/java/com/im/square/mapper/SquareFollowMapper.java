package com.im.square.mapper;

import com.im.square.entity.SquareFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SquareFollowMapper {

    int insert(SquareFollow follow);

    int updateStatus(@Param("followerId") Long followerId,
                     @Param("followeeId") Long followeeId,
                     @Param("status") int status);

    SquareFollow selectOne(@Param("followerId") Long followerId,
                           @Param("followeeId") Long followeeId);

    List<Long> selectFollowerIds(@Param("followeeId") Long followeeId);

    List<Long> selectFolloweeIds(@Param("followerId") Long followerId);

    long countFollowers(@Param("followeeId") Long followeeId);

    long countFollowees(@Param("followerId") Long followerId);
}
