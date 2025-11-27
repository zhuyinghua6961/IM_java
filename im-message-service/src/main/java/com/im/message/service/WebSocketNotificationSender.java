package com.im.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket通知发送器
 * 专门用于发送WebSocket通知，避免循环依赖
 */
@Slf4j
@Component
public class WebSocketNotificationSender {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送通知给指定用户
     */
    public void sendToUser(Long userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(), 
                "/queue/notifications", 
                notification
            );
            log.debug("发送通知给用户: userId={}, notification={}", userId, notification.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("发送用户通知失败: userId={}", userId, e);
        }
    }
    
    /**
     * 发送通知给群组
     */
    public void sendToGroup(Long groupId, Object notification) {
        try {
            messagingTemplate.convertAndSend("/topic/group-notifications-" + groupId, notification);
            log.debug("发送通知给群组: groupId={}, notification={}", groupId, notification.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("发送群组通知失败: groupId={}", groupId, e);
        }
    }
    
    /**
     * 广播通知给所有在线用户
     */
    public void broadcast(Object notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.debug("广播通知: notification={}", notification.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("广播通知失败", e);
        }
    }
}
