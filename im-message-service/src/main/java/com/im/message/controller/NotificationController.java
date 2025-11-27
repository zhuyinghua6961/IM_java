package com.im.message.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

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
}
