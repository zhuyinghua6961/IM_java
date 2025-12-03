package com.im.message.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.common.context.UserContext;
import com.im.message.dto.MessageDTO;
import com.im.message.entity.GroupMember;
import com.im.message.mapper.BlacklistMapper;
import com.im.message.mapper.GroupMemberMapper;
import com.im.message.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 消息处理器
 */
@Slf4j
@Controller
public class WebSocketMessageHandler {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Autowired
    private BlacklistMapper blacklistMapper;
    
    /**
     * 处理客户端发送的消息
     * @MessageMapping("/message") 对应客户端发送到 /app/message
     */
    @MessageMapping("/message")
    public void handleMessage(@Payload Map<String, Object> payload, 
                              SimpMessageHeaderAccessor headerAccessor) {
        try {
            log.info("收到WebSocket消息: {}", payload);
            
            // 1. 从会话中获取用户ID
            Long fromUserId = (Long) headerAccessor.getSessionAttributes().get("userId");
            if (fromUserId == null) {
                log.warn("未找到用户ID，消息被拒绝");
                // 无法获取userId，使用sessionId作为fallback（虽然可能发送失败）
                sendErrorMessage(headerAccessor.getSessionId(), "用户未认证");
                return;
            }
            
            // 2. 设置用户上下文
            UserContext.setCurrentUserId(fromUserId);
            
            // 3. 解析消息内容
            MessageDTO messageDTO = objectMapper.convertValue(payload, MessageDTO.class);
            
            // 4. 单聊时检查黑名单
            if (messageDTO.getChatType() == 1) {
                int blocked = blacklistMapper.isBlockedBy(messageDTO.getToUserId(), fromUserId);
                if (blocked > 0) {
                    log.warn("消息被拦截：发送者 {} 已被接收者 {} 拉黑", fromUserId, messageDTO.getToUserId());
                    
                    // 保存失败消息到数据库（状态码 -1 表示被拉黑）
                    Long messageId = messageService.saveFailedMessage(fromUserId, messageDTO, -1);
                    log.info("被拉黑消息已保存，messageId: {}", messageId);
                    
                    // 发送被拉黑通知给发送方（包含消息ID）
                    sendBlockedMessage(headerAccessor.getSessionId(), messageDTO.getToUserId(), messageId);
                    return;
                }
            }
            
            // 5. 保存消息到数据库
            Long messageId = messageService.sendMessage(fromUserId, messageDTO);
            log.info("=== 消息发送完成 messageId={} ===", messageId);
            
            // 5. 发送ACK确认给发送方（使用真实的sessionId）
            String sessionId = headerAccessor.getSessionId();
            sendAckMessage(sessionId, messageId);
            log.info("=== ACK已发送给前端 sessionId={}, messageId={} ===", sessionId, messageId);
            
            // 6. 推送消息给接收方
            if (messageDTO.getChatType() == 1) {
                // 单聊：推送给指定用户
                pushMessageToUser(messageDTO.getToUserId(), messageId, fromUserId, messageDTO);
                log.info("=== 消息已推送给接收方 messageId={}, toUserId={} ===", messageId, messageDTO.getToUserId());
            } else if (messageDTO.getChatType() == 2) {
                // 群聊：推送给群组所有成员（除了发送者）
                pushMessageToGroup(messageDTO.getGroupId(), messageId, fromUserId, messageDTO);
            }
            
            log.info("消息处理成功，messageId: {}", messageId);
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            // 尝试获取userId发送错误消息
            Long fromUserId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String recipient = fromUserId != null ? String.valueOf(fromUserId) : headerAccessor.getSessionId();
            sendErrorMessage(recipient, "消息发送失败: " + e.getMessage());
        } finally {
            UserContext.clear();
        }
    }
    
    /**
     * 发送ACK确认消息给发送方
     * @param sessionId WebSocket session ID
     * @param messageId 消息ID
     */
    private void sendAckMessage(String sessionId, Long messageId) {
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "ACK");
        // 使用字符串形式的 messageId，避免前端JS精度丢失
        ack.put("messageId", String.valueOf(messageId));
        ack.put("status", "success");
        ack.put("timestamp", System.currentTimeMillis());
        
        // 直接发送到 /queue/ack-{sessionId}
        String destination = "/queue/ack-" + sessionId;
        messagingTemplate.convertAndSend(destination, ack);
        log.info("✅ ACK已发送到: {}, messageId={}", destination, messageId);
    }
    
    /**
     * 发送错误消息
     * @param sessionId WebSocket session ID
     * @param errorMsg 错误消息
     */
    private void sendErrorMessage(String sessionId, String errorMsg) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "ERROR");
        error.put("message", errorMsg);
        error.put("timestamp", System.currentTimeMillis());
        
        String destination = "/queue/error-" + sessionId;
        messagingTemplate.convertAndSend(destination, error);
        log.error("发送错误消息到: {}, error={}", destination, errorMsg);
    }
    
    /**
     * 发送被拉黑提示消息
     * @param sessionId WebSocket session ID
     * @param blockedByUserId 拉黑者的用户ID
     * @param messageId 消息ID（已保存到数据库）
     */
    private void sendBlockedMessage(String sessionId, Long blockedByUserId, Long messageId) {
        Map<String, Object> blocked = new HashMap<>();
        blocked.put("type", "BLOCKED");
        blocked.put("blockedByUserId", blockedByUserId);
        blocked.put("messageId", String.valueOf(messageId));
        blocked.put("message", "对方已将你拉黑，无法发送消息");
        blocked.put("timestamp", System.currentTimeMillis());
        
        String destination = "/queue/error-" + sessionId;
        messagingTemplate.convertAndSend(destination, blocked);
        log.info("发送拉黑提示到: {}, blockedByUserId={}, messageId={}", destination, blockedByUserId, messageId);
    }
    
    /**
     * 推送消息给指定用户（单聊）
     */
    private void pushMessageToUser(Long toUserId, Long messageId, Long fromUserId, MessageDTO messageDTO) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MESSAGE");
        // 使用字符串形式的 messageId
        message.put("messageId", String.valueOf(messageId));
        message.put("fromUserId", fromUserId);
        message.put("chatType", messageDTO.getChatType());
        message.put("msgType", messageDTO.getMsgType());
        message.put("content", messageDTO.getContent());
        message.put("url", messageDTO.getUrl());
        message.put("timestamp", System.currentTimeMillis());
        
        // 发送到用户的私有队列
        messagingTemplate.convertAndSendToUser(
            toUserId.toString(), 
            "/queue/messages", 
            message
        );
        
        log.debug("推送消息给用户: toUserId={}, messageId={}", toUserId, messageId);
    }
    
    /**
     * 推送消息给群组（群聊）
     * 推送给所有群成员（除了发送者）
     */
    private void pushMessageToGroup(Long groupId, Long messageId, Long fromUserId, MessageDTO messageDTO) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MESSAGE");
        // 使用字符串形式的 messageId
        message.put("messageId", String.valueOf(messageId));
        message.put("fromUserId", fromUserId);
        message.put("groupId", groupId);
        message.put("chatType", messageDTO.getChatType());
        message.put("msgType", messageDTO.getMsgType());
        message.put("content", messageDTO.getContent());
        message.put("url", messageDTO.getUrl());
        message.put("timestamp", System.currentTimeMillis());
        
        try {
            // 查询所有群成员
            List<GroupMember> members = groupMemberMapper.selectByGroupId(groupId);
            int sentCount = 0;
            
            // 逐个推送给群成员（排除发送者）
            for (GroupMember member : members) {
                if (!member.getUserId().equals(fromUserId)) {
                    messagingTemplate.convertAndSendToUser(
                        member.getUserId().toString(),
                        "/queue/messages",
                        message
                    );
                    sentCount++;
                }
            }
            
            log.debug("推送群聊消息: groupId={}, messageId={}, 接收者数量={}", groupId, messageId, sentCount);
        } catch (Exception e) {
            log.error("推送群聊消息失败: groupId={}, messageId={}", groupId, messageId, e);
            // 降级：使用广播到群组主题的方式
            messagingTemplate.convertAndSend("/topic/group-" + groupId, message);
            log.info("使用群组主题广播: groupId={}", groupId);
        }
    }
    
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
