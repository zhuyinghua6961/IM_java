package com.im.square.service.impl;

import com.alibaba.fastjson2.JSON;
import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.common.vo.PageResult;
import com.im.square.dto.UserProfileDTO;
import com.im.square.entity.SquareComment;
import com.im.square.entity.SquareLike;
import com.im.square.entity.SquarePost;
import com.im.square.mapper.SquareCommentMapper;
import com.im.square.mapper.SquareLikeMapper;
import com.im.square.mapper.SquarePostMapper;
import com.im.square.service.ContentReviewService;
import com.im.square.service.RemoteUserService;
import com.im.square.service.SquareService;
import com.im.square.vo.SquareCommentVO;
import com.im.square.vo.SquarePostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquareServiceImpl implements SquareService {

    private final SquarePostMapper postMapper;
    private final SquareLikeMapper likeMapper;
    private final SquareCommentMapper commentMapper;
    private final ContentReviewService contentReviewService;
    private final RemoteUserService remoteUserService;

    @Override
    public Long publishPost(Long userId, String title, String content, List<String> images, String video, List<String> tags) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        // 文本内容审核
        contentReviewService.reviewText(content);

        LocalDateTime now = LocalDateTime.now();

        SquarePost post = new SquarePost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setImages(images == null || images.isEmpty() ? null : JSON.toJSONString(images));
        post.setVideo(video);
        post.setTags(tags == null || tags.isEmpty() ? null : JSON.toJSONString(tags));
        // 当前实现：审核通过后才入广场列表
        post.setStatus(1);
        post.setAuditStatus(1);
        post.setAuditReason(null);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCreateTime(now);
        post.setUpdateTime(now);

        postMapper.insert(post);
        log.info("发布广场帖子成功, userId={}, postId={}", userId, post.getId());
        return post.getId();
    }

    @Override
    public PageResult<SquarePostVO> listPublicPosts(Long currentUserId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        int offset = (page - 1) * size;

        List<SquarePost> posts = postMapper.selectPublicList(offset, size);
        long total = postMapper.countPublic();

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            boolean liked = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            records.add(toPostVO(post, liked));
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public SquarePostVO getPostDetail(Long currentUserId, Long postId) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        boolean liked = currentUserId != null && likeMapper.countByPostAndUser(postId, currentUserId) > 0;
        return toPostVO(post, liked);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "只能删除自己发布的帖子");
        }
        int rows = postMapper.softDelete(postId, userId);
        if (rows > 0) {
            log.info("用户删除广场帖子成功, userId={}, postId={}", userId, postId);
        }
    }

    @Override
    public void likePost(Long userId, Long postId) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        int exist = likeMapper.countByPostAndUser(postId, userId);
        if (exist > 0) {
            throw new BusinessException(ResultCode.SQUARE_ALREADY_LIKED);
        }
        SquareLike like = new SquareLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        postMapper.updateCounters(postId, 1, 0);
    }

    @Override
    public void unlikePost(Long userId, Long postId) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        int rows = likeMapper.delete(postId, userId);
        if (rows > 0) {
            postMapper.updateCounters(postId, -1, 0);
        }
    }

    @Override
    public PageResult<SquareCommentVO> listComments(Long postId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        int offset = (page - 1) * size;

        // 确保帖子存在且可见
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }

        List<SquareComment> comments = commentMapper.selectByPost(postId, offset, size);
        long total = commentMapper.countByPost(postId);

        List<SquareCommentVO> records = new ArrayList<>();
        for (SquareComment comment : comments) {
            records.add(toCommentVO(comment));
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public Long addComment(Long userId, Long postId, String content, Long parentId) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评论内容不能为空");
        }
        // 文本内容审核
        contentReviewService.reviewText(content);

        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }

        // 如果是回复，检查被回复的评论是否存在
        if (parentId != null) {
            SquareComment parent = commentMapper.selectById(parentId);
            if (parent == null || parent.getStatus() == null || parent.getStatus() == 0) {
                throw new BusinessException(ResultCode.SQUARE_COMMENT_NOT_FOUND, "被回复的评论不存在");
            }
        }

        SquareComment comment = new SquareComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        postMapper.updateCounters(postId, 0, 1);
        return comment.getId();
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        SquareComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == null || comment.getStatus() == 0) {
            throw new BusinessException(ResultCode.SQUARE_COMMENT_NOT_FOUND);
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "只能删除自己发表的评论");
        }
        int rows = commentMapper.softDelete(commentId, userId);
        if (rows > 0) {
            postMapper.updateCounters(comment.getPostId(), 0, -1);
        }
    }

    @Override
    public PageResult<SquarePostVO> listMyPosts(Long userId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        int offset = (page - 1) * size;

        List<SquarePost> posts = postMapper.selectByUser(userId, offset, size);
        long total = postMapper.countByUser(userId);

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            // 我的帖子列表里，liked 字段目前对自己用处不大，统一返回 false
            records.add(toPostVO(post, false));
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    private SquarePostVO toPostVO(SquarePost post, boolean liked) {
        if (post == null) {
            return null;
        }
        SquarePostVO vo = new SquarePostVO();
        vo.setPostId(post.getId());
        vo.setUserId(post.getUserId());
        UserProfileDTO profile = remoteUserService.getUserProfile(post.getUserId());
        if (profile != null) {
            vo.setNickname(profile.getNickname());
            vo.setAvatar(profile.getAvatar());
        }
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setImages(parseJsonArray(post.getImages()));
        vo.setVideo(post.getVideo());
        vo.setTags(parseJsonArray(post.getTags()));
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setLiked(liked);
        vo.setCreateTime(post.getCreateTime());
        return vo;
    }

    private SquareCommentVO toCommentVO(SquareComment comment) {
        SquareCommentVO vo = new SquareCommentVO();
        vo.setCommentId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        UserProfileDTO profile = remoteUserService.getUserProfile(comment.getUserId());
        if (profile != null) {
            vo.setNickname(profile.getNickname());
            vo.setAvatar(profile.getAvatar());
        }
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
