package com.im.message.controller;

import com.im.common.context.UserContext;
import com.im.common.vo.Result;
import com.im.message.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
public class MessageController {
    
    @Autowired
    private MessageService messageService;

    /**
     * 获取历史消息
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getMessageHistory(
            @RequestParam Long targetId,
            @RequestParam Integer chatType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Long userId = UserContext.getCurrentUserId();
        log.info("获取历史消息: userId={}, targetId={}, chatType={}, page={}, size={}", 
                userId, targetId, chatType, page, size);
        
        Map<String, Object> result = messageService.getHistoryMessages(userId, targetId, chatType, page, size);
        return Result.success(result);
    }

    /**
     * 撤回消息
     */
    @PostMapping("/recall")
    public Result<Void> recallMessage(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        Long messageId = Long.valueOf(request.get("messageId").toString());
        
        log.info("撤回消息: userId={}, messageId={}", userId, messageId);
        
        messageService.recallMessage(messageId, userId);
        return Result.success();
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/read")
    public Result<Void> markAsRead(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Long> messageIds = (List<Long>) request.get("messageIds");
        
        log.info("标记消息已读: userId={}, messageIds={}", userId, messageIds);
        
        messageService.markMessagesAsRead(messageIds, userId);
        return Result.success();
    }
    
    /**
     * 删除单条消息
     */
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("删除消息: messageId={}, userId={}", messageId, userId);
        
        messageService.deleteMessage(messageId, userId);
        return Result.success();
    }
}
