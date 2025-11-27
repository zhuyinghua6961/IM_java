package com.im.message.config;

import com.im.message.interceptor.WebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，消息的前缀为 /topic 和 /queue
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息的前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 指定用户消息的前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }
}
