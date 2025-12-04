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
                     List<String> tags);

    PageResult<SquarePostVO> listPublicPosts(Long currentUserId, int page, int size);

    SquarePostVO getPostDetail(Long currentUserId, Long postId);

    void deletePost(Long userId, Long postId);

    void likePost(Long userId, Long postId);

    void unlikePost(Long userId, Long postId);

    PageResult<SquareCommentVO> listComments(Long postId, int page, int size);

    Long addComment(Long userId, Long postId, String content, Long parentId);

    void deleteComment(Long userId, Long commentId);

    PageResult<SquarePostVO> listMyPosts(Long userId, int page, int size);
}
