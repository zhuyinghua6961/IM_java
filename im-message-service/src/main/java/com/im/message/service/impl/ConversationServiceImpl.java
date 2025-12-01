package com.im.message.service.impl;

import com.im.common.exception.BusinessException;
import com.im.common.enums.ResultCode;
import com.im.message.entity.Conversation;
import com.im.message.mapper.ConversationMapper;
import com.im.message.service.ConversationService;
import com.im.message.vo.ConversationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    
    @Autowired
    private ConversationMapper conversationMapper;
    
    @Autowired
    private com.im.message.mapper.MessageMapper messageMapper;
    
    @Autowired
    private com.im.message.mapper.MessageDeleteMapper messageDeleteMapper;
    
    @Override
    public List<ConversationVO> getConversationList(Long userId) {
        log.debug("获取会话列表，userId: {}", userId);
        
        List<Conversation> conversations = conversationMapper.selectByUserId(userId);
        List<ConversationVO> result = new ArrayList<>();
        
        // TODO: 补充用户/群组信息和最后一条消息内容
        for (Conversation conversation : conversations) {
            ConversationVO vo = new ConversationVO();
            vo.setConversationId(conversation.getId());
            vo.setTargetId(conversation.getTargetId());
            vo.setChatType(conversation.getChatType());
            vo.setUnreadCount(conversation.getUnreadCount());
            vo.setTop(conversation.getTop() != null && conversation.getTop() == 1);
            vo.setHidden(conversation.getHidden() != null && conversation.getHidden() == 1);
            vo.setLastMsgTime(conversation.getUpdateTime());
            result.add(vo);
        }
        
        log.debug("获取会话列表成功，count: {}", result.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnreadCount(Long userId, Long targetId, Integer chatType) {
        log.debug("清空未读数，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        if (conversation != null) {
            conversationMapper.clearUnreadCount(conversation.getId());
            log.debug("清空未读数成功，conversationId: {}", conversation.getId());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topConversation(Long conversationId, Boolean top) {
        log.debug("置顶会话，conversationId: {}, top: {}", conversationId, top);
        
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        
        conversationMapper.updateTop(conversationId, top ? 1 : 0);
        log.debug("置顶会话成功，conversationId: {}", conversationId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrCreateConversation(Long userId, Long targetId, Integer chatType, Long lastMsgId, boolean incrementUnread) {
        log.debug("更新或创建会话，userId: {}, targetId: {}, chatType: {}, lastMsgId: {}, incrementUnread: {}", 
                userId, targetId, chatType, lastMsgId, incrementUnread);
        
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        
        if (conversation == null) {
            // 创建新会话
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setChatType(chatType);
            conversation.setLastMsgId(lastMsgId);
            conversation.setUnreadCount(incrementUnread ? 1 : 0);
            conversation.setTop(0);
            conversation.setHidden(0); // 新会话默认不隐藏
            conversationMapper.insert(conversation);
            log.debug("创建新会话成功，conversationId: {}", conversation.getId());
        } else {
            // 更新现有会话
            conversationMapper.updateLastMessage(conversation.getId(), lastMsgId);
            if (incrementUnread) {
                conversationMapper.incrementUnreadCount(conversation.getId());
            }
            
            // 如果会话被隐藏了，发送新消息时自动显示
            if (conversation.getHidden() != null && conversation.getHidden() == 1) {
                conversationMapper.showConversation(conversation.getId());
                log.debug("会话已自动显示，conversationId: {}", conversation.getId());
            }
            
            log.debug("更新会话成功，conversationId: {}", conversation.getId());
        }
    }
    
    @Override
    public void hideConversation(Long conversationId) {
        log.debug("隐藏会话，conversationId: {}", conversationId);
        conversationMapper.hideConversation(conversationId);
    }
    
    @Override
    public void hideConversationByUserAndTarget(Long userId, Long targetId, Integer chatType) {
        log.debug("根据用户和目标隐藏会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        
        // 先查找会话
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        if (conversation != null) {
            conversationMapper.hideConversation(conversation.getId());
            log.debug("隐藏会话成功，conversationId: {}", conversation.getId());
        } else {
            log.warn("未找到要隐藏的会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        }
    }
    
    @Override
    public void showConversation(Long userId, Long targetId, Integer chatType) {
        log.debug("显示会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        conversationMapper.showConversationByUserAndTarget(userId, targetId, chatType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId, Long userId) {
        log.debug("删除会话，conversationId: {}, userId: {}", conversationId, userId);
        
        // 1. 验证会话是否存在且属于当前用户
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此会话");
        }
        
        // 2. 查询该会话的所有消息ID
        List<com.im.message.entity.Message> messages = messageMapper.selectHistoryMessages(
            userId, 
            conversation.getTargetId(), 
            conversation.getChatType(), 
            0, 
            Integer.MAX_VALUE
        );
        
        // 3. 批量插入删除记录
        if (!messages.isEmpty()) {
            List<com.im.message.entity.MessageDelete> deleteRecords = messages.stream()
                .map(msg -> com.im.message.entity.MessageDelete.builder()
                    .userId(userId)
                    .messageId(msg.getId())
                    .build())
                .collect(java.util.stream.Collectors.toList());
            
            messageDeleteMapper.batchInsert(deleteRecords);
            log.debug("标记 {} 条消息为已删除", deleteRecords.size());
        }
        
        // 4. 删除会话记录
        int result = conversationMapper.deleteById(conversationId);
        if (result > 0) {
            log.info("删除会话成功，conversationId: {}, 标记了 {} 条消息", 
                conversationId, messages.size());
        } else {
            log.warn("删除会话失败，conversationId: {}", conversationId);
            throw new BusinessException(ResultCode.BAD_REQUEST, "删除会话失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversationByUserAndTarget(Long userId, Long targetId, Integer chatType) {
        log.debug("根据用户和目标删除会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        
        // 查找会话
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        if (conversation != null) {
            // 查询该会话的所有消息
            List<com.im.message.entity.Message> messages = messageMapper.selectHistoryMessages(
                userId, targetId, chatType, 0, Integer.MAX_VALUE
            );
            
            // 批量插入删除记录
            if (!messages.isEmpty()) {
                List<com.im.message.entity.MessageDelete> deleteRecords = messages.stream()
                    .map(msg -> com.im.message.entity.MessageDelete.builder()
                        .userId(userId)
                        .messageId(msg.getId())
                        .build())
                    .collect(java.util.stream.Collectors.toList());
                
                messageDeleteMapper.batchInsert(deleteRecords);
                log.debug("标记 {} 条消息为已删除", deleteRecords.size());
            }
            
            // 删除会话
            conversationMapper.deleteById(conversation.getId());
            log.info("删除会话成功，conversationId: {}, 标记了 {} 条消息", 
                conversation.getId(), messages.size());
        } else {
            log.warn("未找到要删除的会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
    }
}
