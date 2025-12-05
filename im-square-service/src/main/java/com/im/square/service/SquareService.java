package com.im.square.service;

import com.im.common.vo.PageResult;
import com.im.square.vo.SquareCommentVO;
import com.im.square.vo.SquarePostVO;
import com.im.square.vo.SquareProfileVO;

import java.util.List;

public interface SquareService {

    Long publishPost(Long userId,
                     String title,
                     String content,
                     List<String> images,
                     String video,
                     List<String> tags,
                     Integer visibleType,
                     List<Long> excludeUserIds);

    PageResult<SquarePostVO> listPublicPosts(Long currentUserId, int page, int size);

    /**
     * 按热度排序的公开广场帖子列表
     */
    PageResult<SquarePostVO> listHotPosts(Long currentUserId, int page, int size);

    /**
     * 获取我关注的用户的广场帖子（基于关注关系的 feed 流）
     */
    PageResult<SquarePostVO> listFollowFeed(Long currentUserId, int page, int size);

    SquarePostVO getPostDetail(Long currentUserId, Long postId);

    void deletePost(Long userId, Long postId);

    void updatePost(Long userId,
                    Long postId,
                    String title,
                    String content,
                    List<String> images,
                    String video,
                    List<String> tags,
                    Integer visibleType,
                    List<Long> excludeUserIds);

    void likePost(Long userId, Long postId);

    void unlikePost(Long userId, Long postId);

    void favoritePost(Long userId, Long postId);

    void unfavoritePost(Long userId, Long postId);

    PageResult<SquareCommentVO> listComments(Long postId, int page, int size);

    Long addComment(Long userId, Long postId, String content, Long parentId);

    void deleteComment(Long userId, Long commentId);

    PageResult<SquarePostVO> listMyPosts(Long userId, int page, int size);

    SquareProfileVO getUserSquareProfile(Long currentUserId, Long targetUserId);

    PageResult<SquarePostVO> listUserPosts(Long currentUserId, Long targetUserId, int page, int size);

    /**
     * 某个用户收藏的帖子列表
     */
    PageResult<SquarePostVO> listUserFavoritePosts(Long currentUserId, Long targetUserId, int page, int size);

    /**
     * 某个用户点赞过的帖子列表
     */
    PageResult<SquarePostVO> listUserLikedPosts(Long currentUserId, Long targetUserId, int page, int size);

    /**
     * 关注某个广场用户（单向关注，不是好友）
     */
    void followUser(Long followerId, Long followeeId);

    /**
     * 取消关注
     */
    void unfollowUser(Long followerId, Long followeeId);
}
