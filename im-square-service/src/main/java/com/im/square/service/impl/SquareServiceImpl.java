package com.im.square.service.impl;

import com.alibaba.fastjson2.JSON;
import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.common.vo.PageResult;
import com.im.square.dto.UserProfileDTO;
import com.im.square.entity.SquareComment;
import com.im.square.entity.SquareLike;
import com.im.square.entity.SquareFavorite;
import com.im.square.entity.SquarePost;
import com.im.square.entity.SquareFollow;
import com.im.square.mapper.SquareCommentMapper;
import com.im.square.mapper.SquareLikeMapper;
import com.im.square.mapper.SquareFavoriteMapper;
import com.im.square.mapper.SquarePostMapper;
import com.im.square.mapper.SquareFollowMapper;
import com.im.square.mapper.FriendRelationMapper;
import com.im.square.service.ContentReviewService;
import com.im.square.service.RemoteSquareNotificationService;
import com.im.square.service.RemoteUserService;
import com.im.square.service.SquareService;
import com.im.square.vo.SquareCommentVO;
import com.im.square.vo.SquarePostVO;
import com.im.square.vo.SquareProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquareServiceImpl implements SquareService {

    private final SquarePostMapper postMapper;
    private final SquareLikeMapper likeMapper;
    private final SquareCommentMapper commentMapper;
    private final ContentReviewService contentReviewService;
    private final RemoteUserService remoteUserService;
    private final RemoteSquareNotificationService remoteSquareNotificationService;
    private final SquareFollowMapper followMapper;
    private final SquareFavoriteMapper favoriteMapper;
    private final FriendRelationMapper friendRelationMapper;

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String PUBLIC_POST_LIST_KEY_PREFIX = "square:post:list:page:";
    private static final long PUBLIC_POST_LIST_EXPIRE_MINUTES = 5L;

    private static final String HOT_POST_LIST_KEY_PREFIX = "square:post:hot:page:";
    private static final long HOT_POST_LIST_EXPIRE_MINUTES = 5L;

    private static final String HOT_POST_ZSET_KEY = "square:post:hot:zset";
    private static final long HOT_POST_ZSET_MAX_SIZE = 500L;

    private static final String FOLLOW_FEED_KEY_PREFIX = "square:feed:user:";
    private static final long FOLLOW_FEED_MAX_SIZE = 500L;

    @Value("${im.square.feed-kafka-topic:im-square-feed}")
    private String feedKafkaTopic;

    @Override
    public Long publishPost(Long userId,
                            String title,
                            String content,
                            List<String> images,
                            String video,
                            List<String> tags,
                            Integer visibleType,
                            List<Long> excludeUserIds) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        // 文本内容审核
        contentReviewService.reviewText(content);

        // 媒体数量校验：图片数量 + (是否有视频) 不得超过 18
        int imageCount = (images == null) ? 0 : images.size();
        boolean hasVideo = video != null && !video.trim().isEmpty();
        int totalMedia = imageCount + (hasVideo ? 1 : 0);
        if (totalMedia > 18) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片和视频总数不能超过 18 个");
        }

        LocalDateTime now = LocalDateTime.now();

        SquarePost post = new SquarePost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setImages(images == null || images.isEmpty() ? null : JSON.toJSONString(images));
        post.setVideo(video);
        post.setTags(tags == null || tags.isEmpty() ? null : JSON.toJSONString(tags));
        // 可见范围：默认公开 0
        post.setVisibleType(visibleType == null ? 0 : visibleType);
        post.setExcludeUsers(excludeUserIds == null || excludeUserIds.isEmpty() ? null : JSON.toJSONString(excludeUserIds));
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

        clearPublicPostListCache();

        // 发送异步Feed事件到Kafka，ID使用字符串避免雪花ID精度问题
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("postId", String.valueOf(post.getId()));
            event.put("authorId", String.valueOf(userId));
            event.put("createTime", now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            String payload = JSON.toJSONString(event);
            kafkaTemplate.send(feedKafkaTopic, String.valueOf(userId), payload);
            log.info("发送广场Feed Kafka事件成功, topic={}, key={}, payload={}", feedKafkaTopic, userId, payload);
        } catch (Exception e) {
            log.warn("发送广场Feed Kafka事件失败, userId={}, postId={}", userId, post.getId(), e);
        }

        return post.getId();
    }

    @Override
    public PageResult<SquarePostVO> listPublicPosts(Long currentUserId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        int offset = (page - 1) * size;

        String key = PUBLIC_POST_LIST_KEY_PREFIX + page + ":" + size;
        List<SquarePost> posts;
        long total;

        try {
            String cache = redisTemplate.opsForValue().get(key);
            if (cache != null && !cache.isEmpty()) {
                CachedPostPage cached = JSON.parseObject(cache, CachedPostPage.class);
                if (cached != null && cached.getPosts() != null) {
                    posts = cached.getPosts();
                    total = cached.getTotal();
                    redisTemplate.expire(key, PUBLIC_POST_LIST_EXPIRE_MINUTES, TimeUnit.MINUTES);
                    log.info("命中广场帖子列表缓存, page={}, size={}, count={}", page, size, posts.size());
                } else {
                    posts = postMapper.selectPublicList(offset, size);
                    total = postMapper.countPublic();
                }
            } else {
                posts = postMapper.selectPublicList(offset, size);
                total = postMapper.countPublic();
            }
        } catch (Exception e) {
            log.warn("读取或解析广场帖子列表缓存失败，降级为数据库查询, page={}, size={}", page, size, e);
            posts = postMapper.selectPublicList(offset, size);
            total = postMapper.countPublic();
        }

        // 当前用户关注的作者ID集合，用于标记 followed
        Set<Long> followeeIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                if (followeeIds != null && !followeeIds.isEmpty()) {
                    followeeIdSet = new HashSet<>(followeeIds);
                }
            } catch (Exception e) {
                log.warn("查询广场关注列表失败, userId={}", currentUserId, e);
            }
        }

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
        }

        try {
            CachedPostPage toCache = new CachedPostPage();
            toCache.setPosts(posts);
            toCache.setTotal(total);
            String json = JSON.toJSONString(toCache);
            redisTemplate.opsForValue().set(key, json, PUBLIC_POST_LIST_EXPIRE_MINUTES, TimeUnit.MINUTES);
            log.info("缓存广场帖子列表, page={}, size={}, count={}", page, size, posts.size());
        } catch (Exception e) {
            log.warn("写入广场帖子列表缓存失败, page={}, size={}", page, size, e);
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public PageResult<SquarePostVO> searchPublicPosts(Long currentUserId,
                                                      String keyword,
                                                      List<String> tags,
                                                      int page,
                                                      int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        int offset = (page - 1) * size;

        String trimmedKeyword = (keyword == null) ? null : keyword.trim();
        if (trimmedKeyword != null && trimmedKeyword.isEmpty()) {
            trimmedKeyword = null;
        }
        List<String> tagList = (tags == null || tags.isEmpty()) ? null : tags;

        List<SquarePost> posts;
        long total;
        try {
            posts = postMapper.searchPublic(trimmedKeyword, tagList, offset, size);
            total = postMapper.countSearchPublic(trimmedKeyword, tagList);
        } catch (Exception e) {
            log.warn("搜索广场帖子失败，降级为空结果, keyword={}, tags={}", trimmedKeyword, tagList, e);
            posts = Collections.emptyList();
            total = 0L;
        }

        // 当前用户关注的作者ID集合，用于标记 followed
        Set<Long> followeeIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                if (followeeIds != null && !followeeIds.isEmpty()) {
                    followeeIdSet = new HashSet<>(followeeIds);
                }
            } catch (Exception e) {
                log.warn("查询广场关注列表失败(搜索), userId={}", currentUserId, e);
            }
        }

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public PageResult<SquarePostVO> listHotPosts(Long currentUserId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        Set<String> idStrSet;
        long total = 0L;
        try {
            idStrSet = redisTemplate.opsForZSet().reverseRange(HOT_POST_ZSET_KEY, start, end);
            Long zcard = redisTemplate.opsForZSet().size(HOT_POST_ZSET_KEY);
            if (zcard != null) {
                total = zcard;
            }
        } catch (Exception e) {
            log.warn("读取热门帖子ZSet失败，降级为数据库查询, page={}, size={}", page, size, e);
            idStrSet = Collections.emptySet();
        }

        // 如果ZSet暂无数据，退回到数据库排序逻辑，保证接口可用
        if (idStrSet == null || idStrSet.isEmpty()) {
            int offset = (page - 1) * size;
            List<SquarePost> posts = postMapper.selectPublicHotList(offset, size);
            long dbTotal = postMapper.countPublic();

            Set<Long> followeeIdSet = Collections.emptySet();
            if (currentUserId != null) {
                try {
                    List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                    if (followeeIds != null && !followeeIds.isEmpty()) {
                        followeeIdSet = new HashSet<>(followeeIds);
                    }
                } catch (Exception e) {
                    log.warn("查询广场关注列表失败(热门-DB), userId={}", currentUserId, e);
                }
            }

            List<SquarePostVO> records = new ArrayList<>();
            for (SquarePost post : posts) {
                if (!isPostVisibleToUser(post, currentUserId)) {
                    continue;
                }
                boolean liked = false;
                boolean favorited = false;
                if (currentUserId != null) {
                    liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                    favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                }
                boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
                SquarePostVO vo = toPostVO(post, liked, followed);
                int favCount = favoriteMapper.countByPost(post.getId());
                vo.setFavoriteCount(favCount);
                vo.setFavorited(favorited);
                records.add(vo);
            }

            return PageResult.of(records, dbTotal, (long) size, (long) page);
        }

        List<Long> ids = new ArrayList<>();
        for (String s : idStrSet) {
            try {
                ids.add(Long.valueOf(s));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        List<SquarePost> posts = postMapper.selectByIds(ids);
        if (posts == null || posts.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        Map<Long, SquarePost> postMap = new HashMap<>();
        for (SquarePost post : posts) {
            postMap.put(post.getId(), post);
        }

        // 当前用户关注的作者ID集合，用于标记 followed
        Set<Long> followeeIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                if (followeeIds != null && !followeeIds.isEmpty()) {
                    followeeIdSet = new HashSet<>(followeeIds);
                }
            } catch (Exception e) {
                log.warn("查询广场关注列表失败(热门-ZSet), userId={}", currentUserId, e);
            }
        }

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        List<SquarePostVO> records = new ArrayList<>();
        for (Long id : ids) {
            SquarePost post = postMap.get(id);
            if (post == null) {
                continue;
            }
            if (post.getCreateTime() == null || post.getCreateTime().isBefore(threeDaysAgo)) {
                continue;
            }
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public SquareProfileVO getUserSquareProfile(Long currentUserId, Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }

        UserProfileDTO profile = remoteUserService.getUserProfile(targetUserId);
        if (profile == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在或不可用");
        }

        SquareProfileVO vo = new SquareProfileVO();
        vo.setUserId(targetUserId);
        vo.setNickname(profile.getNickname());
        vo.setAvatar(profile.getAvatar());

        long fansCount = followMapper.countFollowers(targetUserId);
        long followCount = followMapper.countFollowees(targetUserId);
        long postCount = postMapper.countByUser(targetUserId);
        long likeCount = postMapper.sumLikesByUser(targetUserId);

        vo.setFansCount(fansCount);
        vo.setFollowCount(followCount);
        vo.setPostCount(postCount);
        vo.setLikeCount(likeCount);

        boolean self = currentUserId != null && Objects.equals(currentUserId, targetUserId);
        vo.setSelf(self);

        boolean followed = false;
        if (!self && currentUserId != null) {
            try {
                SquareFollow follow = followMapper.selectOne(currentUserId, targetUserId);
                followed = (follow != null && follow.getStatus() != null && follow.getStatus() == 1);
            } catch (Exception e) {
                log.warn("查询广场主页时关注关系失败, viewerId={}, authorId={}", currentUserId, targetUserId, e);
            }
        }
        vo.setFollowed(followed);

        return vo;
    }

    @Override
    public PageResult<SquarePostVO> listUserPosts(Long currentUserId, Long targetUserId, int page, int size) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        int offset = (page - 1) * size;
        List<SquarePost> posts = postMapper.selectByUser(targetUserId, offset, size);
        long total = postMapper.countByUser(targetUserId);

        boolean followed = false;
        if (currentUserId != null && !Objects.equals(currentUserId, targetUserId)) {
            try {
                SquareFollow follow = followMapper.selectOne(currentUserId, targetUserId);
                followed = (follow != null && follow.getStatus() != null && follow.getStatus() == 1);
            } catch (Exception e) {
                log.warn("查询用户广场帖子列表时关注关系失败, viewerId={}, authorId={}", currentUserId, targetUserId, e);
            }
        }

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public PageResult<SquarePostVO> listUserFavoritePosts(Long currentUserId, Long targetUserId, int page, int size) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        int offset = (page - 1) * size;
        List<Long> postIds = favoriteMapper.selectPostIdsByUser(targetUserId, offset, size);
        long total = favoriteMapper.countByUser(targetUserId);

        if (postIds == null || postIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        List<SquarePost> posts = postMapper.selectByIds(postIds);

        // 当前用户关注的作者ID集合
        Set<Long> followeeIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                if (followeeIds != null && !followeeIds.isEmpty()) {
                    followeeIdSet = new HashSet<>(followeeIds);
                }
            } catch (Exception e) {
                log.warn("查询收藏列表时关注关系列表失败, viewerId={}", currentUserId, e);
            }
        }

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public PageResult<SquarePostVO> listUserLikedPosts(Long currentUserId, Long targetUserId, int page, int size) {
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        int offset = (page - 1) * size;
        List<Long> postIds = likeMapper.selectPostIdsByUser(targetUserId, offset, size);
        long total = likeMapper.countByUser(targetUserId);

        if (postIds == null || postIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        List<SquarePost> posts = postMapper.selectByIds(postIds);

        // 当前用户关注的作者ID集合
        Set<Long> followeeIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                List<Long> followeeIds = followMapper.selectFolloweeIds(currentUserId);
                if (followeeIds != null && !followeeIds.isEmpty()) {
                    followeeIdSet = new HashSet<>(followeeIds);
                }
            } catch (Exception e) {
                log.warn("查询点赞列表时关注关系列表失败, viewerId={}", currentUserId, e);
            }
        }

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = false;
            boolean favorited = false;
            if (currentUserId != null) {
                liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
                favorited = favoriteMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            }
            boolean followed = currentUserId != null && followeeIdSet.contains(post.getUserId());
            SquarePostVO vo = toPostVO(post, liked, followed);
            int favCount = favoriteMapper.countByPost(post.getId());
            vo.setFavoriteCount(favCount);
            vo.setFavorited(favorited);
            records.add(vo);
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
        if (!isPostVisibleToUser(post, currentUserId)) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        boolean liked = currentUserId != null && likeMapper.countByPostAndUser(postId, currentUserId) > 0;
        boolean favorited = currentUserId != null && favoriteMapper.countByPostAndUser(postId, currentUserId) > 0;
        boolean followed = false;
        if (currentUserId != null) {
            try {
                SquareFollow follow = followMapper.selectOne(currentUserId, post.getUserId());
                followed = (follow != null && follow.getStatus() != null && follow.getStatus() == 1);
            } catch (Exception e) {
                log.warn("查询帖子详情时关注关系失败, viewerId={}, authorId={}", currentUserId, post.getUserId(), e);
            }
        }
        SquarePostVO vo = toPostVO(post, liked, followed);
        int favCount = favoriteMapper.countByPost(post.getId());
        vo.setFavoriteCount(favCount);
        vo.setFavorited(favorited);
        return vo;
    }

    @Override
    public void favoritePost(Long userId, Long postId) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        int exist = favoriteMapper.countByPostAndUser(postId, userId);
        if (exist > 0) {
            return;
        }
        SquareFavorite fav = new SquareFavorite();
        fav.setPostId(postId);
        fav.setUserId(userId);
        fav.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(fav);
        adjustHotScore(post, 4D);
        clearPublicPostListCache();
    }

    @Override
    public void unfavoritePost(Long userId, Long postId) {
        favoriteMapper.delete(postId, userId);
        adjustHotScore(postId, -4D);
        clearPublicPostListCache();
    }

    @Override
    public void updatePost(Long userId,
                           Long postId,
                           String title,
                           String content,
                           List<String> images,
                           String video,
                           List<String> tags,
                           Integer visibleType,
                           List<Long> excludeUserIds) {
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0) {
            throw new BusinessException(ResultCode.SQUARE_POST_NOT_FOUND);
        }
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "只能编辑自己发布的帖子");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "内容不能为空");
        }
        contentReviewService.reviewText(content);

        int imageCount = (images == null) ? 0 : images.size();
        boolean hasVideo = video != null && !video.trim().isEmpty();
        int totalMedia = imageCount + (hasVideo ? 1 : 0);
        if (totalMedia > 18) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "图片和视频总数不能超过 18 个");
        }

        post.setTitle(title);
        post.setContent(content);
        post.setImages(images == null || images.isEmpty() ? null : JSON.toJSONString(images));
        post.setVideo(video);
        post.setTags(tags == null || tags.isEmpty() ? null : JSON.toJSONString(tags));
        post.setVisibleType(visibleType == null ? 0 : visibleType);
        post.setExcludeUsers(excludeUserIds == null || excludeUserIds.isEmpty() ? null : JSON.toJSONString(excludeUserIds));
        post.setUpdateTime(LocalDateTime.now());

        int rows = postMapper.updatePost(post);
        if (rows > 0) {
            log.info("用户编辑广场帖子成功, userId={}, postId={}", userId, postId);
            clearPublicPostListCache();
        }
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
            clearPublicPostListCache();
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
        // 发送点赞通知
        remoteSquareNotificationService.sendLikeNotification(post, userId);
        adjustHotScore(post, 2D);
        clearPublicPostListCache();
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
            adjustHotScore(post, -2D);
        }
        clearPublicPostListCache();
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
        // 发送评论通知
        remoteSquareNotificationService.sendCommentNotification(post, userId, comment.getId(), content);
        adjustHotScore(post, 3D);
        clearPublicPostListCache();
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
            adjustHotScore(comment.getPostId(), -3D);
            clearPublicPostListCache();
        }
    }

    private void adjustHotScore(SquarePost post, double delta) {
        if (post == null || post.getId() == null) {
            return;
        }
        try {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            if (post.getCreateTime() == null || post.getCreateTime().isBefore(threeDaysAgo)) {
                redisTemplate.opsForZSet().remove(HOT_POST_ZSET_KEY, String.valueOf(post.getId()));
                return;
            }

            String member = String.valueOf(post.getId());
            redisTemplate.opsForZSet().incrementScore(HOT_POST_ZSET_KEY, member, delta);

            Long size = redisTemplate.opsForZSet().size(HOT_POST_ZSET_KEY);
            if (size != null && size > HOT_POST_ZSET_MAX_SIZE) {
                long removeCount = size - HOT_POST_ZSET_MAX_SIZE;
                if (removeCount > 0) {
                    redisTemplate.opsForZSet().removeRange(HOT_POST_ZSET_KEY, 0, removeCount - 1);
                }
            }
        } catch (Exception e) {
            log.warn("调整热门帖子热度失败, postId={}", post.getId(), e);
        }
    }

    private void adjustHotScore(Long postId, double delta) {
        if (postId == null) {
            return;
        }
        SquarePost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() == 0 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            try {
                redisTemplate.opsForZSet().remove(HOT_POST_ZSET_KEY, String.valueOf(postId));
            } catch (Exception e) {
                log.warn("从热门帖子ZSet移除失效帖子失败, postId={}", postId, e);
            }
            return;
        }
        adjustHotScore(post, delta);
    }

    private void clearPublicPostListCache() {
        try {
            Set<String> keys = new HashSet<>();

            Set<String> publicKeys = redisTemplate.keys(PUBLIC_POST_LIST_KEY_PREFIX + "*");
            if (publicKeys != null && !publicKeys.isEmpty()) {
                keys.addAll(publicKeys);
            }

            Set<String> hotKeys = redisTemplate.keys(HOT_POST_LIST_KEY_PREFIX + "*");
            if (hotKeys != null && !hotKeys.isEmpty()) {
                keys.addAll(hotKeys);
            }

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理广场帖子列表缓存失败", e);
        }
    }

    private static class CachedPostPage {
        private List<SquarePost> posts;
        private long total;

        public List<SquarePost> getPosts() {
            return posts;
        }

        public void setPosts(List<SquarePost> posts) {
            this.posts = posts;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
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
            // 我的帖子列表里，liked / followed 字段对自己用处不大，统一返回 false
            records.add(toPostVO(post, false, false));
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    @Override
    public PageResult<SquarePostVO> listFollowFeed(Long currentUserId, int page, int size) {
        if (currentUserId == null) {
            return PageResult.of(Collections.emptyList(), 0L, (long) size, (long) page);
        }
        if (page <= 0) page = 1;
        if (size <= 0) size = 20;

        String key = FOLLOW_FEED_KEY_PREFIX + currentUserId;
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        Set<String> idStrSet;
        long total = 0L;
        try {
            idStrSet = redisTemplate.opsForZSet().reverseRange(key, start, end);
            Long zcard = redisTemplate.opsForZSet().size(key);
            if (zcard != null) {
                total = zcard;
            }
        } catch (Exception e) {
            log.warn("读取关注 feed 失败，降级为空, userId={}, page={}, size={}", currentUserId, page, size, e);
            idStrSet = Collections.emptySet();
        }

        if (idStrSet == null || idStrSet.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        List<Long> ids = new ArrayList<>();
        for (String s : idStrSet) {
            try {
                ids.add(Long.valueOf(s));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, (long) size, (long) page);
        }

        List<SquarePost> posts = postMapper.selectByIds(ids);

        List<SquarePostVO> records = new ArrayList<>();
        for (SquarePost post : posts) {
            if (!isPostVisibleToUser(post, currentUserId)) {
                continue;
            }
            boolean liked = likeMapper.countByPostAndUser(post.getId(), currentUserId) > 0;
            // 来自“我的关注”feed，作者必然是已关注
            records.add(toPostVO(post, liked, true));
        }

        return PageResult.of(records, total, (long) size, (long) page);
    }

    private SquarePostVO toPostVO(SquarePost post, boolean liked, boolean followed) {
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
        vo.setVisibleType(post.getVisibleType());
        vo.setExcludeUserIds(parseLongArray(post.getExcludeUsers()));
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setImages(parseJsonArray(post.getImages()));
        vo.setVideo(post.getVideo());
        vo.setTags(parseJsonArray(post.getTags()));
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setLiked(liked);
        vo.setFollowed(followed);
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
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

    private List<Long> parseLongArray(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, Long.class);
        } catch (Exception e) {
            log.warn("解析 Long 数组失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 判断帖子对当前用户是否可见
     */
    private boolean isPostVisibleToUser(SquarePost post, Long viewerId) {
        if (post == null || post.getStatus() == null || post.getStatus() != 1 ||
                post.getAuditStatus() == null || post.getAuditStatus() != 1) {
            return false;
        }

        // 作者自己永远可见
        if (viewerId != null && Objects.equals(post.getUserId(), viewerId)) {
            return true;
        }

        // 未登录用户视为游客
        if (viewerId != null && isExcludedForUser(post, viewerId)) {
            return false;
        }

        Integer vt = post.getVisibleType();
        if (vt == null || vt == 0) {
            // 公开：所有人可见（排除名单之外）
            return true;
        } else if (vt == 1) {
            // 仅好友：作者自己 + 好友可见
            if (viewerId == null) {
                return false;
            }
            return friendRelationMapper.countFriendship(viewerId, post.getUserId()) > 0;
        } else {
            // 未知类型，按公开处理
            return true;
        }
    }

    private boolean isExcludedForUser(SquarePost post, Long viewerId) {
        if (viewerId == null) {
            return false;
        }
        String json = post.getExcludeUsers();
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            List<Long> ids = JSON.parseArray(json, Long.class);
            if (ids == null || ids.isEmpty()) {
                return false;
            }
            for (Long id : ids) {
                if (Objects.equals(id, viewerId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("解析排除可见用户列表失败: {}", json, e);
        }
        return false;
    }

    @Override
    public void followUser(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null || Objects.equals(followerId, followeeId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关注用户参数不合法");
        }
        SquareFollow exist = followMapper.selectOne(followerId, followeeId);
        if (exist == null) {
            SquareFollow follow = new SquareFollow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            follow.setStatus(1);
            follow.setCreateTime(LocalDateTime.now());
            follow.setUpdateTime(LocalDateTime.now());
            followMapper.insert(follow);
        } else {
            followMapper.updateStatus(followerId, followeeId, 1);
        }
    }

    @Override
    public void unfollowUser(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null || Objects.equals(followerId, followeeId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "取消关注用户参数不合法");
        }
        followMapper.updateStatus(followerId, followeeId, 0);
    }
}
