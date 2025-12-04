package com.im.message.controller;

import com.im.common.context.UserContext;
import com.im.common.vo.Result;
import com.im.message.entity.SquareNotification;
import com.im.message.mapper.SquareNotificationMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 广场通知控制器
 * 提供广场点赞/评论通知的创建和查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/notification/square")
public class SquareNotificationController {

    @Autowired
    private SquareNotificationMapper squareNotificationMapper;

    /**
     * 创建一条广场通知（供 square-service 调用）
     */
    @PostMapping
    public Result<Void> createNotification(@RequestBody SquareNotificationCreateDTO dto) {
        try {
            SquareNotification n = new SquareNotification();
            n.setUserId(dto.getToUserId());
            n.setPostId(dto.getPostId());
            n.setCommentId(dto.getCommentId());
            n.setActorId(dto.getActorId());
            n.setActionType(dto.getActionType());
            n.setMessage(dto.getMessage());
            n.setExtra(dto.getExtra());
            n.setRead(0);
            n.setCreateTime(LocalDateTime.now());

            squareNotificationMapper.insert(n);
            log.info("创建广场通知成功: toUserId={}, actionType={}, postId={}, commentId={}",
                    dto.getToUserId(), dto.getActionType(), dto.getPostId(), dto.getCommentId());
            return Result.success();
        } catch (Exception e) {
            log.error("创建广场通知失败", e);
            return Result.error("创建广场通知失败");
        }
    }

    /**
     * 获取当前用户的广场通知列表
     */
    @GetMapping
    public Result<Map<String, Object>> listNotifications(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
        int offset = (page - 1) * size;

        List<SquareNotification> list = squareNotificationMapper.selectByUserId(userId, offset, size);
        int total = squareNotificationMapper.countByUserId(userId);
        int unread = squareNotificationMapper.countUnreadByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("unread", unread);

        return Result.success(result);
    }

    /**
     * 将当前用户的所有广场通知标记为已读
     */
    @PostMapping("/read")
    public Result<Void> markAllRead() {
        Long userId = UserContext.getCurrentUserId();
        int updated = squareNotificationMapper.markAllReadByUserId(userId);
        log.info("标记广场通知已读: userId={}, updated={}", userId, updated);
        return Result.success();
    }

    @Data
    public static class SquareNotificationCreateDTO {
        /** 接收通知的用户ID */
        private Long toUserId;
        /** 帖子ID */
        private Long postId;
        /** 评论ID，可为空 */
        private Long commentId;
        /** 触发动作的用户ID */
        private Long actorId;
        /** 动作类型 LIKE/COMMENT */
        private String actionType;
        /** 通知文案（已拼好） */
        private String message;
        /** 扩展信息 JSON 字符串 */
        private String extra;
    }
}
