package com.im.square.controller;

import com.im.common.context.UserContext;
import com.im.common.vo.PageResult;
import com.im.common.vo.Result;
import com.im.square.service.SquareService;
import com.im.square.vo.SquareCommentVO;
import com.im.square.vo.SquarePostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/square")
@RequiredArgsConstructor
public class SquareController {

    private final SquareService squareService;

    /**
     * 发布广场帖子
     */
    @PostMapping("/posts")
    public Result<Map<String, Object>> publishPost(@RequestBody Map<String, Object> params) {
        Long userId = UserContext.getCurrentUserId();
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) params.get("images");
        String video = (String) params.get("video");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) params.get("tags");

        Integer visibleType = null;
        Object vtObj = params.get("visibleType");
        if (vtObj instanceof Number) {
            visibleType = ((Number) vtObj).intValue();
        } else if (vtObj instanceof String) {
            String s = ((String) vtObj).trim();
            if (!"".equals(s)) {
                try {
                    visibleType = Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        List<Long> excludeUserIds = null;
        Object excludeObj = params.get("excludeUserIds");
        if (excludeObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) excludeObj;
            if (!rawList.isEmpty()) {
                excludeUserIds = new java.util.ArrayList<>();
                for (Object o : rawList) {
                    if (o == null) continue;
                    if (o instanceof Number) {
                        excludeUserIds.add(((Number) o).longValue());
                    } else if (o instanceof String) {
                        String s = ((String) o).trim();
                        if (!"".equals(s)) {
                            try {
                                excludeUserIds.add(Long.parseLong(s));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                if (excludeUserIds.isEmpty()) {
                    excludeUserIds = null;
                }
            }
        }

        log.info("发布广场帖子: userId={}, title={}", userId, title);

        Long postId = squareService.publishPost(userId, title, content, images, video, tags, visibleType, excludeUserIds);
        return Result.success(Map.of("postId", postId));
    }

    /**
     * 获取广场帖子列表
     */
    @GetMapping("/posts")
    public PageResult<SquarePostVO> listPosts(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                              @RequestParam(name = "size", defaultValue = "20") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取广场帖子列表: userId={}, page={}, size={}", userId, page, size);
        return squareService.listPublicPosts(userId, page, size);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/posts/{postId}")
    public Result<SquarePostVO> getPostDetail(@PathVariable("postId") Long postId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取广场帖子详情: userId={}, postId={}", userId, postId);
        SquarePostVO vo = squareService.getPostDetail(userId, postId);
        return Result.success(vo);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(@PathVariable("postId") Long postId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("删除广场帖子: userId={}, postId={}", userId, postId);
        squareService.deletePost(userId, postId);
        return Result.success();
    }

    /**
     * 点赞帖子
     */
    @PostMapping("/posts/{postId}/like")
    public Result<Void> likePost(@PathVariable("postId") Long postId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("点赞广场帖子: userId={}, postId={}", userId, postId);
        squareService.likePost(userId, postId);
        return Result.success();
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/posts/{postId}/like")
    public Result<Void> unlikePost(@PathVariable("postId") Long postId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("取消点赞广场帖子: userId={}, postId={}", userId, postId);
        squareService.unlikePost(userId, postId);
        return Result.success();
    }

    /**
     * 获取帖子评论列表
     */
    @GetMapping("/posts/{postId}/comments")
    public PageResult<SquareCommentVO> listComments(@PathVariable("postId") Long postId,
                                                    @RequestParam(name = "page", defaultValue = "1") Integer page,
                                                    @RequestParam(name = "size", defaultValue = "20") Integer size) {
        log.info("获取广场评论列表: postId={}, page={}, size={}", postId, page, size);
        return squareService.listComments(postId, page, size);
    }

    /**
     * 发表评论 / 回复评论
     */
    @PostMapping("/posts/{postId}/comments")
    public Result<Map<String, Object>> addComment(@PathVariable("postId") Long postId,
                                                  @RequestBody Map<String, Object> params) {
        Long userId = UserContext.getCurrentUserId();
        String content = (String) params.get("content");
        Long parentId = null;
        Object parentIdObj = params.get("parentId");
        if (parentIdObj != null) {
            parentId = Long.valueOf(parentIdObj.toString());
        }

        log.info("新增广场评论: userId={}, postId={}, parentId={}", userId, postId, parentId);
        Long commentId = squareService.addComment(userId, postId, content, parentId);
        return Result.success(Map.of("commentId", commentId));
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable("commentId") Long commentId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("删除广场评论: userId={}, commentId={}", userId, commentId);
        squareService.deleteComment(userId, commentId);
        return Result.success();
    }

    /**
     * 我的帖子列表
     */
    @GetMapping("/my/posts")
    public PageResult<SquarePostVO> listMyPosts(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                @RequestParam(name = "size", defaultValue = "20") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取我的广场帖子列表: userId={}, page={}, size={}", userId, page, size);
        return squareService.listMyPosts(userId, page, size);
    }

    /**
     * 获取我关注的用户的广场帖子（关注 feed）
     */
    @GetMapping("/feed")
    public PageResult<SquarePostVO> listFollowFeed(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                   @RequestParam(name = "size", defaultValue = "20") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取我的关注广场帖子列表: userId={}, page={}, size={}", userId, page, size);
        return squareService.listFollowFeed(userId, page, size);
    }

    /**
     * 关注广场用户（单向关注）
     */
    @PostMapping("/follow/{targetUserId}")
    public Result<Void> followUser(@PathVariable("targetUserId") Long targetUserId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("关注广场用户: followerId={}, followeeId={}", userId, targetUserId);
        squareService.followUser(userId, targetUserId);
        return Result.success();
    }

    /**
     * 取消关注广场用户
     */
    @DeleteMapping("/follow/{targetUserId}")
    public Result<Void> unfollowUser(@PathVariable("targetUserId") Long targetUserId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("取消关注广场用户: followerId={}, followeeId={}", userId, targetUserId);
        squareService.unfollowUser(userId, targetUserId);
        return Result.success();
    }
}
