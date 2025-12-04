package com.im.square.service;

import com.im.common.vo.PageResult;
import com.im.square.vo.SquareCommentVO;
import com.im.square.vo.SquarePostVO;

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
     * 获取我关注的用户的广场帖子（基于关注关系的 feed 流）
     */
    PageResult<SquarePostVO> listFollowFeed(Long currentUserId, int page, int size);

    SquarePostVO getPostDetail(Long currentUserId, Long postId);

    void deletePost(Long userId, Long postId);

    void likePost(Long userId, Long postId);

    void unlikePost(Long userId, Long postId);

    PageResult<SquareCommentVO> listComments(Long postId, int page, int size);

    Long addComment(Long userId, Long postId, String content, Long parentId);

    void deleteComment(Long userId, Long commentId);

    PageResult<SquarePostVO> listMyPosts(Long userId, int page, int size);

    /**
     * 关注某个广场用户（单向关注，不是好友）
     */
    void followUser(Long followerId, Long followeeId);

    /**
     * 取消关注
     */
    void unfollowUser(Long followerId, Long followeeId);
}
