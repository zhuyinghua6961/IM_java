package com.im.message.interceptor;

import com.im.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器 - 验证Token并提取用户信息
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");
            
            log.info("WebSocket握手请求, token: {}", token != null ? "存在" : "不存在");
            
            // 验证Token并提取用户ID
            if (StringUtils.hasText(token)) {
                try {
                    Long userId = JwtUtil.getUserIdFromToken(token);
                    attributes.put("userId", userId);
                    attributes.put("token", token);
                    log.info("WebSocket握手成功, userId: {}", userId);
                    return true;
                } catch (Exception e) {
                    log.warn("Token验证失败: {}", e.getMessage());
                    return false;
                }
            } else {
                log.warn("Token为空，拒绝WebSocket连接");
                return false;
            }
        }
        
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手失败", exception);
        } else {
            log.info("WebSocket握手完成");
        }
    }
}
