package com.im.message.controller;

import com.im.common.context.UserContext;
import com.im.common.vo.Result;
import com.im.message.service.ConversationService;
import com.im.message.vo.ConversationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;

    /**
     * 获取会话列表
     */
    @GetMapping("/list")
    public Result<List<ConversationVO>> getConversationList() {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取会话列表, userId={}", userId);
        
        List<ConversationVO> conversations = conversationService.getConversationList(userId);
        return Result.success(conversations);
    }

    /**
     * 清空未读数
     */
    @PostMapping("/clear-unread")
    public Result<Void> clearUnread(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        Long targetId = Long.valueOf(request.get("targetId").toString());
        Integer chatType = Integer.valueOf(request.get("chatType").toString());
        
        log.info("清空未读数: userId={}, targetId={}, chatType={}", userId, targetId, chatType);
        
        conversationService.clearUnreadCount(userId, targetId, chatType);
        return Result.success();
    }

    /**
     * 置顶会话
     */
    @PostMapping("/top")
    public Result<Void> topConversation(@RequestBody Map<String, Object> request) {
        Long conversationId = Long.valueOf(request.get("conversationId").toString());
        Boolean top = (Boolean) request.get("top");
        
        log.info("置顶会话: conversationId={}, top={}", conversationId, top);
        
        conversationService.topConversation(conversationId, top);
        return Result.success();
    }

    /**
     * 隐藏会话
     */
    @PostMapping("/hide")
    public Result<Void> hideConversation(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        String conversationIdStr = request.get("conversationId").toString();
        
        log.info("隐藏会话: userId={}, conversationId={}", userId, conversationIdStr);
        
        // 解析前端传递的会话ID格式：chatType-targetId
        if (conversationIdStr.contains("-")) {
            String[] parts = conversationIdStr.split("-");
            Integer chatType = Integer.valueOf(parts[0]);
            Long targetId = Long.valueOf(parts[1]);
            
            log.info("解析会话参数: chatType={}, targetId={}", chatType, targetId);
            
            conversationService.hideConversationByUserAndTarget(userId, targetId, chatType);
        } else {
            // 如果是纯数字，按原来的逻辑处理
            Long conversationId = Long.valueOf(conversationIdStr);
            conversationService.hideConversation(conversationId);
        }
        
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("删除会话: conversationId={}, userId={}", conversationId, userId);
        
        // 判断conversationId格式：可能是纯数字ID，也可能是 "chatType-targetId" 格式
        if (conversationId.contains("-")) {
            // 格式: "chatType-targetId"
            String[] parts = conversationId.split("-");
            Integer chatType = Integer.valueOf(parts[0]);
            Long targetId = Long.valueOf(parts[1]);
            
            log.info("解析会话参数: chatType={}, targetId={}", chatType, targetId);
            conversationService.deleteConversationByUserAndTarget(userId, targetId, chatType);
        } else {
            // 格式: 纯数字ID
            Long id = Long.valueOf(conversationId);
            conversationService.deleteConversation(id, userId);
        }
        
        return Result.success();
    }
}
