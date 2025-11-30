package com.im.message.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket通道拦截器 - 设置用户Principal
 */
@Slf4j
@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP连接时，从session attributes获取userId并设置为user
            Long userId = (Long) accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                // 创建一个简单的Principal实现
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return String.valueOf(userId);
                    }
                });
                log.info("WebSocket CONNECT - 设置user principal: {}", userId);
            }
        }
        
        return message;
    }
}
