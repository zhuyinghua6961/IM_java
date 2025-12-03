package com.im.user.mapper;

import com.im.user.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface FriendMapper {
    int insert(Friend friend);
    int updateById(Friend friend);
    Friend selectByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);
    Friend selectActiveFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
    List<Friend> selectByUserId(@Param("userId") Long userId);
    int deleteById(@Param("id") Long id);
    List<Map<String, Object>> selectFriendListWithUserInfo(@Param("userId") Long userId);
    
    /**
     * 更新好友免打扰状态
     */
    int updateMuted(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("muted") Integer muted);
    
    /**
     * 查询免打扰好友列表
     */
    List<Map<String, Object>> selectMutedFriendList(@Param("userId") Long userId);
}
