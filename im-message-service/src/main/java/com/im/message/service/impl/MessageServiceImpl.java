package com.im.message.service.impl;

import com.im.common.exception.BusinessException;
import com.im.common.enums.ResultCode;
import com.im.common.util.SnowflakeIdGenerator;
import com.im.message.constant.RedisKeyConstant;
import com.im.message.dto.MessageDTO;
import com.im.message.entity.Message;
import com.im.message.mapper.MessageMapper;
import com.im.message.entity.GroupMember;
import com.im.message.mapper.GroupMemberMapper;
import com.im.message.service.MessageService;
import com.im.message.service.ConversationService;
import com.im.message.service.MessageNotificationService;
import com.im.message.service.MessageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    
    @Autowired
    private com.im.message.mapper.MessageDeleteMapper messageDeleteMapper;
    
    @Autowired
    private MessageCacheService messageCacheService;
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
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
        message.setAtUserIds(messageDTO.getAtUserIds()); // 保存@的用户ID
        message.setStatus(1); // 正常状态
        message.setSendTime(LocalDateTime.now());
        
        // 单聊：异步持久化；群聊：同步持久化
        if (messageDTO.getChatType() == 1) {
            // 单聊：使用雪花算法生成ID，异步持久化
            message.setId(idGenerator.nextId());
            message.setPersistStatus(RedisKeyConstant.PERSIST_STATUS_PENDING);
            
            // 缓存到Redis并发送到Kafka
            messageCacheService.cacheAndSendToKafka(message);
            log.debug("单聊消息已缓存，将异步持久化: messageId={}", message.getId());
        } else {
            // 群聊：保持同步写库（群聊消息量大，需要额外优化，暂不使用异步）
            // 为群聊消息同样使用雪花ID，并通过Kafka+Redis异步持久化
            message.setId(idGenerator.nextId());
            message.setPersistStatus(RedisKeyConstant.PERSIST_STATUS_PENDING);

            messageCacheService.cacheAndSendToKafka(message);
            log.debug("群聊消息已缓存，将异步持久化: messageId={}", message.getId());
        }
        
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
    @Transactional(rollbackFor = Exception.class)
    public Long saveFailedMessage(Long fromUserId, MessageDTO messageDTO, Integer failureStatus) {
        log.info("保存失败消息，fromUserId: {}, failureStatus: {}", fromUserId, failureStatus);
        
        // 1. 确定接收方ID
        Long toId;
        if (messageDTO.getChatType() == 1) {
            toId = messageDTO.getToUserId();
        } else {
            toId = messageDTO.getGroupId();
        }
        
        // 2. 创建消息记录（设置失败状态）
        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setFromUserId(fromUserId);
        message.setToId(toId);
        message.setChatType(messageDTO.getChatType());
        message.setMsgType(messageDTO.getMsgType());
        message.setContent(messageDTO.getContent());
        message.setUrl(messageDTO.getUrl());
        message.setStatus(failureStatus); // -1 被拉黑，-2 其他失败
        message.setSendTime(LocalDateTime.now());
        message.setPersistStatus(RedisKeyConstant.PERSIST_STATUS_PENDING);
        
        // 3. 缓存到Redis并发送到Kafka（异步持久化）
        messageCacheService.cacheAndSendToKafka(message);
        log.debug("失败消息已缓存，将异步持久化: messageId={}", message.getId());
        
        // 4. 只更新发送方的会话（不更新接收方，因为消息未送达）
        conversationService.updateOrCreateConversation(
            fromUserId, toId, messageDTO.getChatType(), message.getId(), false
        );
        
        log.info("失败消息保存成功，messageId: {}, status: {}", message.getId(), failureStatus);
        return message.getId();
    }
    
    @Override
    public Map<String, Object> getHistoryMessages(Long userId, Long targetId, Integer chatType, Integer page, Integer size) {
        log.debug("获取历史消息，userId: {}, targetId: {}, chatType: {}, page: {}, size: {}", 
                userId, targetId, chatType, page, size);
        
        // 生成会话ID
        String conversationId = chatType == 1 
            ? messageCacheService.generateConversationId(userId, targetId)
            : String.valueOf(targetId);
        
        List<Message> resultMessages = new ArrayList<>();
        
        // 检查缓存是否已加载
        boolean cacheLoaded = messageCacheService.isConversationCacheLoaded(conversationId);
        
        if (cacheLoaded) {
            // 缓存已加载，直接从Redis获取
            List<Message> cachedMessages = messageCacheService.getConversationMessagesFromCache(conversationId, size);
            
            // 过滤已删除的消息
            for (Message msg : cachedMessages) {
                if (!messageCacheService.isMessageDeleted(userId, msg.getId())) {
                    resultMessages.add(msg);
                }
            }
            log.debug("从Redis缓存获取消息: conversationId={}, count={}", conversationId, resultMessages.size());
        }
        
        // 缓存未加载或不够，从MySQL获取
        if (!cacheLoaded || resultMessages.size() < size) {
            int offset = (page - 1) * size;
            // 加载较多消息用于缓存
            int loadSize = Math.max(size, 500);
            List<Message> dbMessages = messageMapper.selectHistoryMessages(userId, targetId, chatType, offset, loadSize);
            
            log.debug("从MySQL查询消息: count={}", dbMessages.size());
            
            // 如果缓存未加载，将MySQL数据加载到Redis
            if (!cacheLoaded && !dbMessages.isEmpty()) {
                messageCacheService.loadHistoryMessagesToCache(conversationId, dbMessages);
                messageCacheService.markConversationCacheLoaded(conversationId);
            }
            
            // 合并去重
            Set<Long> existIds = resultMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toSet());
            
            for (Message msg : dbMessages) {
                if (!existIds.contains(msg.getId())) {
                    resultMessages.add(msg);
                }
            }
        }
        
        // 按时间倒序排序
        resultMessages.sort((m1, m2) -> m2.getSendTime().compareTo(m1.getSendTime()));
        
        // 分页处理
        int total = messageMapper.countHistoryMessages(userId, targetId, chatType);
        int endIndex = Math.min(size, resultMessages.size());
        List<Message> pagedMessages = resultMessages.subList(0, endIndex);
        
        // 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", pagedMessages);
        
        log.debug("获取历史消息成功，total: {}, listSize: {}", total, pagedMessages.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Long userId) {
        log.info("撤回消息，messageId: {}, userId: {}", messageId, userId);
        
        try {
            // 1. 查询消息（先查Redis，再查MySQL）
            Message message = messageCacheService.getMessageFromCache(messageId);
            log.debug("从Redis查询消息: messageId={}, result={}", messageId, message != null ? "找到" : "未找到");
            
            if (message == null) {
                message = messageMapper.selectById(messageId);
                log.debug("从MySQL查询消息: messageId={}, result={}", messageId, message != null ? "找到" : "未找到");
            }
            
            if (message == null) {
                log.warn("消息不存在（Redis和MySQL都未找到），messageId: {}", messageId);
                throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
            }
            
            log.debug("查询到消息: fromUserId={}, chatType={}, status={}, persistStatus={}, sendTime={}", 
                    message.getFromUserId(), message.getChatType(), message.getStatus(), 
                    message.getPersistStatus(), message.getSendTime());
            
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
            
            log.debug("时间检查: 发送时间={}, 当前时间={}, 相差{}分钟", sendTime, now, diffMinutes);
            
            if (diffMinutes > 5) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "消息发送超过5分钟，无法撤回");
            }
            
            // 5. 撤回消息（单聊和群聊分别处理）
            if (message.getChatType() == 1) {
                // 单聊：根据persistStatus处理
                String persistStatus = message.getPersistStatus() != null ? 
                    message.getPersistStatus() : RedisKeyConstant.PERSIST_STATUS_PERSISTED;
                
                messageCacheService.markMessageAsRecalled(messageId, persistStatus);
                log.info("单聊消息撤回成功（异步），messageId={}, persistStatus={}", messageId, persistStatus);
            } else {
                // 群聊：保持同步撤回（改为通过Kafka+Redis异步撤回）
                String persistStatus = message.getPersistStatus() != null ? 
                    message.getPersistStatus() : RedisKeyConstant.PERSIST_STATUS_PERSISTED;

                messageCacheService.markMessageAsRecalled(messageId, persistStatus);
                log.info("群聊消息撤回成功（异步），messageId={}, persistStatus={}", messageId, persistStatus);
            }
            
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
        log.debug("标记消息已读，userId: {}, messageCount: {}", userId, messageIds.size());
        
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        try {
            messageMapper.batchInsertReadRecords(messageIds, userId);
            log.debug("标记消息已读成功");
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            // 如果是重复记录，不抛出异常
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId, Long userId) {
        log.info("删除单条消息，messageId: {}, userId: {}", messageId, userId);
        
        // 1. 验证消息是否存在
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        
        // 2. 验证用户权限（单聊双方都可删除，群聊成员可删除）
        if (message.getChatType() == 1) {
            // 单聊：发送方和接收方都可以删除
            if (!message.getFromUserId().equals(userId) && !message.getToId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此消息");
            }
        } else if (message.getChatType() == 2) {
            // 群聊：验证是否是群成员
            GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(message.getToId(), userId);
            if (member == null) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此消息");
            }
        }
        
        // 3. 插入删除记录（数据库）
        com.im.message.entity.MessageDelete deleteRecord = com.im.message.entity.MessageDelete.builder()
            .userId(userId)
            .messageId(messageId)
            .build();
        
        messageDeleteMapper.insert(deleteRecord);
        
        // 4. 同时更新Redis缓存
        messageCacheService.markMessageAsDeleted(userId, messageId);
        
        log.info("删除消息成功（数据库+Redis），messageId: {}, userId: {}", messageId, userId);
    }
    
    @Override
    public Map<String, Object> searchMessages(Long userId, String keyword, Integer chatType, Long targetId, Integer page, Integer size) {
        log.info("搜索消息，userId: {}, keyword: {}, chatType: {}, targetId: {}, page: {}, size: {}", 
                userId, keyword, chatType, targetId, page, size);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "搜索关键词不能为空");
        }
        
        // 获取用户所在的所有群组ID
        List<GroupMember> groupMembers = groupMemberMapper.selectByUserId(userId);
        List<Long> groupIds = groupMembers.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toList());
        
        int offset = (page - 1) * size;
        
        // 搜索消息
        List<Message> messages = messageMapper.searchMessages(
                userId, keyword.trim(), groupIds, chatType, targetId, null, null, offset, size);
        
        // 统计总数
        int total = messageMapper.countSearchMessages(
                userId, keyword.trim(), groupIds, chatType, targetId, null, null);
        
        // 转换为返回格式（id 转为 String 避免 JavaScript 精度丢失）
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", String.valueOf(msg.getId()));
            item.put("fromUserId", String.valueOf(msg.getFromUserId()));
            item.put("toId", String.valueOf(msg.getToId()));
            item.put("chatType", msg.getChatType());
            item.put("msgType", msg.getMsgType());
            item.put("content", msg.getContent());
            item.put("sendTime", msg.getSendTime());
            resultList.add(item);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", resultList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getMessageContext(Long userId, Long messageId, Integer contextSize) {
        log.info("获取消息上下文，userId: {}, messageId: {}, contextSize: {}", userId, messageId, contextSize);
        
        // 1. 先从数据库查询目标消息
        Message targetMessage = messageMapper.selectById(messageId);
        if (targetMessage == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        
        // 2. 确定会话信息
        Integer chatType = targetMessage.getChatType();
        Long targetId;
        if (chatType == 1) {
            // 单聊：确定对方ID
            targetId = targetMessage.getFromUserId().equals(userId) 
                ? targetMessage.getToId() 
                : targetMessage.getFromUserId();
        } else {
            // 群聊：使用群ID
            targetId = targetMessage.getToId();
        }
        
        // 3. 查询消息上下文
        List<Message> contextMessages = messageMapper.selectMessageContext(
            userId, targetId, chatType, messageId, contextSize
        );
        
        // 4. 生成会话ID并缓存到Redis
        String conversationId = chatType == 1 
            ? messageCacheService.generateConversationId(userId, targetId)
            : String.valueOf(targetId);
        
        if (!contextMessages.isEmpty()) {
            messageCacheService.loadHistoryMessagesToCache(conversationId, contextMessages);
            messageCacheService.markConversationCacheLoaded(conversationId);
        }
        
        // 5. 按时间倒序排序
        contextMessages.sort((m1, m2) -> m2.getSendTime().compareTo(m1.getSendTime()));
        
        // 6. 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", contextMessages);
        result.put("targetId", targetId);
        result.put("chatType", chatType);
        result.put("messageId", messageId);
        
        log.info("获取消息上下文成功，消息数: {}", contextMessages.size());
        return result;
    }
}
