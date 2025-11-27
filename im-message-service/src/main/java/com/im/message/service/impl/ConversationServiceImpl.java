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
    
    @Override
    public List<ConversationVO> getConversationList(Long userId) {
        log.info("获取会话列表，userId: {}", userId);
        
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
        
        log.info("获取会话列表成功，count: {}", result.size());
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnreadCount(Long userId, Long targetId, Integer chatType) {
        log.info("清空未读数，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        if (conversation != null) {
            conversationMapper.clearUnreadCount(conversation.getId());
            log.info("清空未读数成功");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topConversation(Long conversationId, Boolean top) {
        log.info("置顶会话，conversationId: {}, top: {}", conversationId, top);
        
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        
        conversationMapper.updateTop(conversationId, top ? 1 : 0);
        log.info("置顶会话成功");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrCreateConversation(Long userId, Long targetId, Integer chatType, Long lastMsgId, boolean incrementUnread) {
        log.info("更新或创建会话，userId: {}, targetId: {}, chatType: {}, lastMsgId: {}, incrementUnread: {}", 
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
            log.info("创建新会话成功，conversationId: {}", conversation.getId());
        } else {
            // 更新现有会话
            conversationMapper.updateLastMessage(conversation.getId(), lastMsgId);
            if (incrementUnread) {
                conversationMapper.incrementUnreadCount(conversation.getId());
            }
            
            // 如果会话被隐藏了，发送新消息时自动显示
            if (conversation.getHidden() != null && conversation.getHidden() == 1) {
                conversationMapper.showConversation(conversation.getId());
                log.info("会话已自动显示，conversationId: {}", conversation.getId());
            }
            
            log.info("更新会话成功，conversationId: {}", conversation.getId());
        }
    }
    
    @Override
    public void hideConversation(Long conversationId) {
        log.info("隐藏会话，conversationId: {}", conversationId);
        conversationMapper.hideConversation(conversationId);
    }
    
    @Override
    public void hideConversationByUserAndTarget(Long userId, Long targetId, Integer chatType) {
        log.info("根据用户和目标隐藏会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        
        // 先查找会话
        Conversation conversation = conversationMapper.selectByUserIdAndTarget(userId, targetId, chatType);
        if (conversation != null) {
            conversationMapper.hideConversation(conversation.getId());
            log.info("隐藏会话成功，conversationId: {}", conversation.getId());
        } else {
            log.warn("未找到要隐藏的会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        }
    }
    
    @Override
    public void showConversation(Long userId, Long targetId, Integer chatType) {
        log.info("显示会话，userId: {}, targetId: {}, chatType: {}", userId, targetId, chatType);
        conversationMapper.showConversationByUserAndTarget(userId, targetId, chatType);
    }
}
