package com.im.ai.controller;

import com.im.ai.entity.AiChatMessage;
import com.im.ai.entity.AiConversation;
import com.im.ai.service.AiChatService;
import com.im.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@Tag(name = "AI聊天", description = "AI聊天相关接口")
public class AiController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiService aiService;

    /**
     * 创建新会话
     */
    @PostMapping("/conversation/create")
    @Operation(summary = "创建新会话", description = "创建一个新的AI聊天会话")
    public Map<String, Object> createConversation(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String firstMessage = request.get("message");

        if (userIdStr == null || userIdStr.isEmpty()) {
            return Map.of("code", 400, "success", false, "message", "用户ID不能为空");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return Map.of("code", 400, "success", false, "message", "无效的用户ID");
        }

        try {
            AiConversation conversation = aiChatService.createConversation(userId, firstMessage);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "data", Map.of("conversationId", conversation.getId()),
                    "message", "success"
            );
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Map.of("code", 500, "success", false, "message", "创建会话失败");
        }
    }

    /**
     * 发送消息（带会话）
     */
    @PostMapping("/conversation/chat")
    @Operation(summary = "发送消息", description = "在会话中发送消息并获取AI回复")
    public Map<String, Object> chatInConversation(@RequestBody Map<String, String> request) {
        String conversationIdStr = request.get("conversationId");
        String message = request.get("message");

        if (conversationIdStr == null || conversationIdStr.isEmpty()) {
            return Map.of("code", 400, "success", false, "message", "会话ID不能为空");
        }
        if (message == null || message.trim().isEmpty()) {
            return Map.of("code", 400, "success", false, "message", "消息内容不能为空");
        }

        Long conversationId;
        try {
            conversationId = Long.parseLong(conversationIdStr);
        } catch (NumberFormatException e) {
            return Map.of("code", 400, "success", false, "message", "无效的会话ID");
        }

        try {
            AiConversation conversation = aiChatService.getConversation(conversationId);
            if (conversation == null) {
                return Map.of("code", 404, "success", false, "message", "会话不存在");
            }

            String reply = aiChatService.sendMessage(conversation, message);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "reply", reply,
                    "message", "success"
            );
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Map.of("code", 500, "success", false, "message", "发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversation/list")
    @Operation(summary = "获取会话列表", description = "获取用户的所有会话")
    public Map<String, Object> getConversationList(@RequestParam("userId") Long userId) {
        try {
            List<AiConversation> conversations = aiChatService.getConversationList(userId);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "data", conversations,
                    "message", "success"
            );
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Map.of("code", 500, "success", false, "message", "获取会话列表失败");
        }
    }

    /**
     * 获取会话消息
     */
    @GetMapping("/conversation/messages")
    @Operation(summary = "获取会话消息", description = "获取指定会话的所有消息")
    public Map<String, Object> getConversationMessages(@RequestParam("conversationId") Long conversationId) {
        try {
            List<AiChatMessage> messages = aiChatService.getConversationMessages(conversationId);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "data", messages,
                    "message", "success"
            );
        } catch (Exception e) {
            log.error("获取会话消息失败", e);
            return Map.of("code", 500, "success", false, "message", "获取会话消息失败");
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversation")
    @Operation(summary = "删除会话", description = "删除指定的会话")
    public Map<String, Object> deleteConversation(@RequestParam("conversationId") Long conversationId) {
        try {
            aiChatService.deleteConversation(conversationId);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "message", "删除成功"
            );
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Map.of("code", 500, "success", false, "message", "删除会话失败");
        }
    }

    /**
     * 简单对话（不保存会话，兼容旧版本）
     */
    @PostMapping("/simpleChat")
    @Operation(summary = "简单对话", description = "不检索知识库的简单AI对话")
    public Map<String, Object> simpleChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Map.of("code", 400, "success", false, "message", "消息内容不能为空");
        }

        try {
            String reply = aiChatService.simpleChat(message);
            return Map.of(
                    "code", 200,
                    "success", true,
                    "reply", reply,
                    "message", "success"
            );
        } catch (Exception e) {
            log.error("简单对话失败", e);
            return Map.of("code", 500, "success", false, "message", "服务暂时不可用");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "im-ai-service"
        );
    }
}
