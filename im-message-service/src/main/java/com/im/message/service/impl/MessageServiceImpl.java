package com.im.message.service.impl;

import com.im.common.exception.BusinessException;
import com.im.common.enums.ResultCode;
import com.im.message.dto.MessageDTO;
import com.im.message.entity.Message;
import com.im.message.mapper.MessageMapper;
import com.im.message.entity.GroupMember;
import com.im.message.mapper.GroupMemberMapper;
import com.im.message.service.MessageService;
import com.im.message.service.ConversationService;
import com.im.message.service.MessageNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private MessageNotificationService notificationService;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(Long fromUserId, MessageDTO messageDTO) {
        log.info("发送消息，fromUserId: {}, chatType: {}, msgType: {}", 
                fromUserId, messageDTO.getChatType(), messageDTO.getMsgType());
        
        // 1. 验证参数
        if (messageDTO.getChatType() == null || messageDTO.getMsgType() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "聊天类型和消息类型不能为空");
        }
        
        // 2. 确定接收方ID
        Long toId;
        if (messageDTO.getChatType() == 1) {
            // 单聊
            if (messageDTO.getToUserId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "接收用户ID不能为空");
            }
            toId = messageDTO.getToUserId();
        } else if (messageDTO.getChatType() == 2) {
            // 群聊
            if (messageDTO.getGroupId() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "群组ID不能为空");
            }
            toId = messageDTO.getGroupId();
            
            // 验证发送者是否是群成员
            GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(toId, fromUserId);
            if (member == null || member.getStatus() == null || member.getStatus() != 1) {
                throw new BusinessException(ResultCode.FORBIDDEN, "您不是该群的成员，无法发送消息");
            }
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的聊天类型");
        }
        
        // 3. 创建消息记录
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToId(toId);
        message.setChatType(messageDTO.getChatType());
        message.setMsgType(messageDTO.getMsgType());
        message.setContent(messageDTO.getContent());
        message.setUrl(messageDTO.getUrl());
        message.setStatus(1); // 正常状态
        message.setSendTime(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        // 4. 更新会话
        // 发送方的会话
        conversationService.updateOrCreateConversation(
            fromUserId, toId, messageDTO.getChatType(), message.getId(), false
        );
        
        // 接收方的会话
        if (messageDTO.getChatType() == 1) {
            // 单聊：更新接收者的会话
            conversationService.updateOrCreateConversation(
                toId, fromUserId, messageDTO.getChatType(), message.getId(), true
            );
        } else if (messageDTO.getChatType() == 2) {
            // 群聊：更新所有群成员的会话（除了发送者）
            List<GroupMember> members = groupMemberMapper.selectByGroupId(toId);
            for (GroupMember member : members) {
                if (!member.getUserId().equals(fromUserId)) {
                    conversationService.updateOrCreateConversation(
                        member.getUserId(), toId, messageDTO.getChatType(), message.getId(), true
                    );
                }
            }
        }
        
        log.info("消息发送成功，messageId: {}", message.getId());
        return message.getId();
    }
    
    @Override
    public Map<String, Object> getHistoryMessages(Long userId, Long targetId, Integer chatType, Integer page, Integer size) {
        log.info("获取历史消息，userId: {}, targetId: {}, chatType: {}, page: {}, size: {}", 
                userId, targetId, chatType, page, size);
        
        // 1. 计算分页参数
        int offset = (page - 1) * size;
        
        // 2. 查询消息列表
        List<Message> messages = messageMapper.selectHistoryMessages(userId, targetId, chatType, offset, size);
        
        // 3. 查询总数
        int total = messageMapper.countHistoryMessages(userId, targetId, chatType);
        
        // 4. 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", messages);
        
        log.info("获取历史消息成功，total: {}, listSize: {}", total, messages.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Long userId) {
        log.info("撤回消息，messageId: {}, userId: {}", messageId, userId);
        
        try {
            // 1. 查询消息
            Message message = messageMapper.selectById(messageId);
            if (message == null) {
                log.warn("消息不存在，messageId: {}", messageId);
                throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
            }
            
            log.info("查询到消息: fromUserId={}, status={}, sendTime={}", 
                    message.getFromUserId(), message.getStatus(), message.getSendTime());
            
            // 2. 验证权限
            if (!message.getFromUserId().equals(userId)) {
                log.warn("权限验证失败，消息发送者: {}, 当前用户: {}", message.getFromUserId(), userId);
                throw new BusinessException(ResultCode.FORBIDDEN, "只能撤回自己发送的消息");
            }
            
            // 3. 检查消息状态
            if (message.getStatus() != 1) {
                log.warn("消息状态异常，当前状态: {}", message.getStatus());
                throw new BusinessException(ResultCode.BAD_REQUEST, "消息已被撤回或状态异常");
            }
            
            // 4. 检查时间限制（5分钟内可以撤回）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sendTime = message.getSendTime();
            long diffMinutes = java.time.Duration.between(sendTime, now).toMinutes();
            
            log.info("时间检查: 发送时间={}, 当前时间={}, 相差{}分钟", sendTime, now, diffMinutes);
            
            if (diffMinutes > 5) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "消息发送超过5分钟，无法撤回");
            }
            
            // 5. 撤回消息
            log.info("执行撤回操作，messageId: {}, userId: {}", messageId, userId);
            int result = messageMapper.recallMessage(messageId, userId);
            
            log.info("撤回操作结果: {}", result);
            
            if (result == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "撤回消息失败，可能消息不存在或已被撤回");
            }
            
            log.info("消息撤回成功，messageId: {}", messageId);
            
            // 6. 发送撤回通知
            try {
                notificationService.notifyMessageRecalled(messageId, userId, message.getToId(), message.getChatType());
            } catch (Exception e) {
                log.error("发送撤回通知失败", e);
                // 不影响撤回操作的成功
            }
            
        } catch (BusinessException e) {
            log.error("撤回消息业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("撤回消息系统异常", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "服务器内部错误");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesAsRead(List<Long> messageIds, Long userId) {
        log.info("标记消息已读，userId: {}, messageIds: {}", userId, messageIds);
        
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        // 批量插入已读记录
        messageMapper.batchInsertReadRecords(messageIds, userId);
        
        log.info("标记消息已读成功，count: {}", messageIds.size());
    }
}
