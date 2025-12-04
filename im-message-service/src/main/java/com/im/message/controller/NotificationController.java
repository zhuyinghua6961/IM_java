package com.im.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.common.context.UserContext;
import com.im.common.vo.Result;
import com.im.message.entity.GroupNotification;
import com.im.message.mapper.GroupNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知推送控制器
 * 接收来自用户服务的通知请求，通过WebSocket推送给客户端
 */
@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GroupNotificationMapper groupNotificationMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 推送通知到指定用户
     * @param notification 通知内容
     */
    @PostMapping("/push")
    public void pushNotification(@RequestBody Map<String, Object> notification) {
        try {
            Long toUserId = getLongValue(notification.get("toUserId"));
            String type = (String) notification.get("type");
            
            log.info("收到推送请求: type={}, toUserId={}", type, toUserId);

            // 对群相关通知做持久化，作为群消息通知历史
            saveGroupNotificationIfNeeded(notification);
            
            // 推送到指定用户的通知队列
            messagingTemplate.convertAndSendToUser(
                toUserId.toString(),
                "/queue/notifications",
                notification
            );
            
            log.info("通知推送成功: {}", notification);
        } catch (Exception e) {
            log.error("通知推送失败", e);
        }
    }

    /**
     * 获取当前用户的群消息通知历史
     */
    @GetMapping("/group")
    public Result<Map<String, Object>> getGroupNotifications(@RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "50") Integer size) {
        Long userId = UserContext.getCurrentUserId();
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 50;
        }
        int offset = (page - 1) * size;

        List<GroupNotification> list = groupNotificationMapper.selectByUserId(userId, offset, size);
        int total = groupNotificationMapper.countByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 将当前用户的群消息通知标记为已读
     */
    @PostMapping("/group/read")
    public Result<Void> markGroupNotificationsAsRead() {
        Long userId = UserContext.getCurrentUserId();
        int updated = groupNotificationMapper.markAllReadByUserId(userId);
        log.info("标记群通知已读: userId={}, updated={}", userId, updated);
        return Result.success();
    }
    
    /**
     * 安全地将Object转换为Long
     */
    private Long getLongValue(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("无法转换为Long: {}", obj);
            return null;
        }
    }

    /**
     * 如果是群相关通知，则持久化到 group_notification 表
     */
    private void saveGroupNotificationIfNeeded(Map<String, Object> notification) {
        try {
            Object typeObj = notification.get("type");
            Object toUserIdObj = notification.get("toUserId");
            Object groupIdObj = notification.get("groupId");

            if (typeObj == null || toUserIdObj == null || groupIdObj == null) {
                return;
            }

            String type = String.valueOf(typeObj);
            if (!isGroupNotificationType(type)) {
                return;
            }

            Long toUserId = getLongValue(toUserIdObj);
            Long groupId = getLongValue(groupIdObj);
            if (toUserId == null || groupId == null) {
                return;
            }

            GroupNotification entity = new GroupNotification();
            entity.setUserId(toUserId);
            entity.setGroupId(groupId);
            entity.setType(type);
            Object messageObj = notification.get("message");
            entity.setMessage(messageObj != null ? messageObj.toString() : "");
            try {
                entity.setExtra(objectMapper.writeValueAsString(notification));
            } catch (Exception e) {
                entity.setExtra(null);
            }
            entity.setRead(0);
            entity.setCreateTime(LocalDateTime.now());

            groupNotificationMapper.insert(entity);
            log.info("群通知已持久化: userId={}, groupId={}, type={}", toUserId, groupId, type);
        } catch (Exception e) {
            log.error("保存群通知失败: {}", notification, e);
        }
    }

    /**
     * 判断是否为群相关通知类型
     */
    private boolean isGroupNotificationType(String type) {
        return "GROUP_ADMIN_CHANGE".equals(type)
                || "GROUP_MEMBER_REMOVED".equals(type)
                || "GROUP_MEMBER_QUIT".equals(type)
                || "GROUP_DIRECT_JOIN".equals(type)
                || "GROUP_DISSOLVED".equals(type)
                || "GROUP_OWNER_TRANSFER".equals(type)
                || "GROUP_INFO_UPDATE".equals(type)
                || "GROUP_NEW_MEMBER".equals(type)
                || "GROUP_MEMBER_MUTED".equals(type);
    }
}
