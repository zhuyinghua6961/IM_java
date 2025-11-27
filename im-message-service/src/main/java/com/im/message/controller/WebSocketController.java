package com.im.message.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket消息处理器
 */
@Slf4j
@Controller
public class WebSocketController {

    /**
     * 处理客户端发送的消息
     */
    @MessageMapping("/send")
    public void handleMessage(Map<String, Object> message) {
        log.info("收到消息: {}", message);
        
        // TODO: 后续实现完整的消息处理逻辑
        // 1. 保存消息到数据库
        // 2. 推送消息给接收方
        // 3. 返回ACK确认
        
        // 暂时只记录日志
    }

    /**
     * 处理心跳
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Map<String, Object> heartbeat) {
        log.debug("收到心跳: {}", heartbeat);
        // 心跳处理，保持连接
    }
}
