package com.im.square.service;

import com.alibaba.fastjson2.JSON;
import com.im.square.dto.UserProfileDTO;
import com.im.square.entity.SquarePost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 远程广场通知服务，用于调用 message-service 写入广场通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteSquareNotificationService {

    private final RestTemplate restTemplate;
    private final RemoteUserService remoteUserService;

    @Value("${message-service.url:http://localhost:8082}")
    private String messageServiceUrl;

    /**
     * 发送点赞通知
     */
    public void sendLikeNotification(SquarePost post, Long actorId) {
        if (post == null || post.getUserId() == null || Objects.equals(post.getUserId(), actorId)) {
            return;
        }
        try {
            UserProfileDTO actorProfile = remoteUserService.getUserProfile(actorId);
            String nickname = actorProfile != null && actorProfile.getNickname() != null
                    ? actorProfile.getNickname()
                    : "用户 " + actorId;
            String title = post.getTitle() != null ? post.getTitle() : "";
            String message = nickname + " 赞了你的帖子" + (title.isEmpty() ? "" : "《" + title + "》");

            Map<String, Object> extra = new HashMap<>();
            extra.put("actorNickname", nickname);
            extra.put("postTitle", title);

            doSend(post.getUserId(), post.getId(), null, actorId, "LIKE", message, JSON.toJSONString(extra));
        } catch (Exception e) {
            log.warn("发送点赞广场通知失败, postId={}, actorId={}",
                    post != null ? post.getId() : null, actorId, e);
        }
    }

    /**
     * 发送评论通知
     */
    public void sendCommentNotification(SquarePost post, Long actorId, Long commentId, String commentContent) {
        if (post == null || post.getUserId() == null || Objects.equals(post.getUserId(), actorId)) {
            return;
        }
        try {
            UserProfileDTO actorProfile = remoteUserService.getUserProfile(actorId);
            String nickname = actorProfile != null && actorProfile.getNickname() != null
                    ? actorProfile.getNickname()
                    : "用户 " + actorId;
            String title = post.getTitle() != null ? post.getTitle() : "";
            String shortContent = commentContent;
            if (shortContent != null && shortContent.length() > 50) {
                shortContent = shortContent.substring(0, 50) + "...";
            }
            String message = nickname + " 评论了你的帖子" + (title.isEmpty() ? "" : "《" + title + "》")
                    + (shortContent != null ? "：" + shortContent : "");

            Map<String, Object> extra = new HashMap<>();
            extra.put("actorNickname", nickname);
            extra.put("postTitle", title);
            extra.put("commentContent", commentContent);

            doSend(post.getUserId(), post.getId(), commentId, actorId, "COMMENT", message, JSON.toJSONString(extra));
        } catch (Exception e) {
            log.warn("发送评论广场通知失败, postId={}, actorId={}, commentId={}",
                    post != null ? post.getId() : null, actorId, commentId, e);
        }
    }

    public void sendFollowPostNotification(SquarePost post, Long followerId) {
        if (post == null || followerId == null || post.getUserId() == null) {
            return;
        }
        try {
            Long authorId = post.getUserId();
            if (Objects.equals(authorId, followerId)) {
                return;
            }
            UserProfileDTO authorProfile = remoteUserService.getUserProfile(authorId);
            String nickname = authorProfile != null && authorProfile.getNickname() != null
                    ? authorProfile.getNickname()
                    : "用户 " + authorId;
            String title = post.getTitle() != null ? post.getTitle() : "";
            String message = nickname + " 发布了新帖子" + (title.isEmpty() ? "" : "《" + title + "》");

            Map<String, Object> extra = new HashMap<>();
            extra.put("actorNickname", nickname);
            extra.put("postTitle", title);

            doSend(followerId, post.getId(), null, authorId, "FOLLOW_POST", message, JSON.toJSONString(extra));
        } catch (Exception e) {
            log.warn("发送关注用户发帖广场通知失败, postId={}, followerId={}",
                    post != null ? post.getId() : null, followerId, e);
        }
    }

    private void doSend(Long toUserId, Long postId, Long commentId, Long actorId,
                        String actionType, String message, String extraJson) {
        try {
            String url = messageServiceUrl + "/api/notification/square";

            Map<String, Object> body = new HashMap<>();
            body.put("toUserId", toUserId);
            body.put("postId", postId);
            body.put("commentId", commentId);
            body.put("actorId", actorId);
            body.put("actionType", actionType);
            body.put("message", message);
            body.put("extra", extraJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, entity, String.class);
        } catch (Exception e) {
            log.warn("调用消息服务创建广场通知失败, toUserId={}, postId={}, actionType={}",
                    toUserId, postId, actionType, e);
        }
    }
}
