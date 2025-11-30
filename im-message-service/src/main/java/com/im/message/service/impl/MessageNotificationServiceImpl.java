package com.im.message.service.impl;

import com.im.message.dto.MessageNotificationDTO;
import com.im.message.entity.Message;
import com.im.message.service.MessageNotificationService;
import com.im.message.service.WebSocketNotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现
 */
@Slf4j
@Service
public class MessageNotificationServiceImpl implements MessageNotificationService {
    
    @Autowired
    private WebSocketNotificationSender notificationSender;
    
    @Override
    public void notifyMessageRecalled(Long messageId, Long fromUserId, Long targetId, Integer chatType) {
        log.info("通知消息撤回: messageId={}, fromUserId={}, targetId={}, chatType={}", 
                messageId, fromUserId, targetId, chatType);
        
        MessageNotificationDTO notification = MessageNotificationDTO.builder()
                .type(MessageNotificationDTO.TYPE_MESSAGE_RECALLED)
                // 以字符串形式下发 messageId
                .messageId(String.valueOf(messageId))
                .fromUserId(fromUserId)
                .targetId(targetId)
                .chatType(chatType)
                .conversationId(generateConversationId(chatType, targetId))
                .recallTime(LocalDateTime.now())
                .timestamp(System.currentTimeMillis())
                .build();
        
        // 推送给相关用户
        sendNotificationToUsers(notification, fromUserId, targetId, chatType);
    }
    
    @Override
    public void notifyMessagesRead(List<Long> messageIds, Long userId, Long targetId, Integer chatType) {
        log.info("通知消息已读: messageIds={}, userId={}, targetId={}, chatType={}", 
                messageIds, userId, targetId, chatType);
        
        MessageNotificationDTO notification = MessageNotificationDTO.builder()
                .type(MessageNotificationDTO.TYPE_MESSAGES_READ)
                // 将 Long 列表转换为字符串列表
                .messageIds(messageIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .fromUserId(userId)
                .targetId(targetId)
                .chatType(chatType)
                .conversationId(generateConversationId(chatType, targetId))
                .timestamp(System.currentTimeMillis())
                .build();
        
        // 推送给相关用户
        sendNotificationToUsers(notification, userId, targetId, chatType);
    }
    
    @Override
    public void notifyNewMessage(Message message) {
        log.info("通知新消息: messageId={}, fromUserId={}, targetId={}, chatType={}", 
                message.getId(), message.getFromUserId(), message.getToId(), message.getChatType());
        
        MessageNotificationDTO notification = MessageNotificationDTO.builder()
                .type(MessageNotificationDTO.TYPE_NEW_MESSAGE)
                // 以字符串形式下发 messageId
                .messageId(String.valueOf(message.getId()))
                .fromUserId(message.getFromUserId())
                .targetId(message.getToId())
                .chatType(message.getChatType())
                .conversationId(generateConversationId(message.getChatType(), message.getToId()))
                .content(message.getContent())
                .timestamp(System.currentTimeMillis())
                .build();
        
        // 推送给相关用户
        sendNotificationToUsers(notification, message.getFromUserId(), message.getToId(), message.getChatType());
    }
    
    /**
     * 发送通知给相关用户
     */
    private void sendNotificationToUsers(MessageNotificationDTO notification, Long fromUserId, Long targetId, Integer chatType) {
        try {
            if (chatType == 1) {
                // 单聊：推送给发送者和接收者
                notificationSender.sendToUser(fromUserId, notification);
                notificationSender.sendToUser(targetId, notification);
                log.debug("单聊通知已发送: fromUserId={}, targetId={}", fromUserId, targetId);
            } else if (chatType == 2) {
                // 群聊：推送给群组所有成员
                notificationSender.sendToGroup(targetId, notification);
                log.debug("群聊通知已发送: groupId={}", targetId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket通知失败", e);
        }
    }
    
    /**
     * 生成会话ID
     */
    private String generateConversationId(Integer chatType, Long targetId) {
        return chatType + "-" + targetId;
    }
}
