package com.im.ai.service;

import com.im.ai.entity.AiChatMessage;
import com.im.ai.entity.AiConversation;
import com.im.ai.entity.Faq;
import com.im.ai.mapper.AiChatMessageMapper;
import com.im.ai.mapper.AiConversationMapper;
import com.im.ai.util.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天服务
 */
@Slf4j
@Service
public class AiChatService {

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiChatMessageMapper messageMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DeepSeekClient deepSeekClient;

    private static final String FAQ_CACHE_LOCK = "im:ai:faq:lock";

    // 缓存Key前缀
    private static final String CONVERSATION_LIST_CACHE = "im:ai:conv:list:";
    private static final String CONVERSATION_MESSAGES_CACHE = "im:ai:conv:messages:";

    // 缓存过期时间：30分钟
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 创建新会话
     */
    @Transactional
    public AiConversation createConversation(Long userId, String firstMessage) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(firstMessage.length() > 50 ? firstMessage.substring(0, 50) + "..." : firstMessage);
        conversation.setMessageCount(0);
        conversation.setStatus(1);
        conversationMapper.insert(conversation);

        // 清除会话列表缓存
        clearConversationListCache(userId);

        return conversation;
    }

    /**
     * 发送消息并获取回复
     */
    @Transactional
    public String sendMessage(AiConversation conversation, String userMessage) {
        // 1. 保存用户消息
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setConversationId(conversation.getId());
        userMsg.setUserId(conversation.getUserId());
        userMsg.setRole(AiChatMessage.ROLE_USER);
        userMsg.setContent(userMessage);
        messageMapper.insert(userMsg);

        // 2. 检索知识库
        List<Faq> relatedFaqs = searchRelatedFaqs(userMessage);

        // 3. 构建提示词
        String prompt = buildPrompt(userMessage, relatedFaqs);

        // 4. 调用LLM
        String answer = deepSeekClient.chat(userMessage, prompt);

        // 5. 保存AI回复
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setConversationId(conversation.getId());
        aiMsg.setUserId(conversation.getUserId());
        aiMsg.setRole(AiChatMessage.ROLE_ASSISTANT);
        aiMsg.setContent(answer);
        messageMapper.insert(aiMsg);

        // 6. 更新会话标题（第一条消息时）和消息数
        int messageCount = messageMapper.countByConversationId(conversation.getId());
        conversation.setMessageCount(messageCount);
        if (conversation.getMessageCount() <= 2) { // 用户消息 + AI回复
            conversationMapper.updateTitleAndCount(conversation.getId(), userMessage, messageCount);
        } else {
            conversationMapper.updateTitleAndCount(conversation.getId(), conversation.getTitle(), messageCount);
        }

        // 清除相关缓存
        clearConversationMessagesCache(conversation.getId());
        clearConversationListCache(conversation.getUserId());

        return answer;
    }

    /**
     * 简单聊天（不保存会话）
     */
    public String simpleChat(String message) {
        return deepSeekClient.chat(message, "你是一个智能聊天助手，请用简洁友好的语气回答用户问题，回答控制在100字以内。");
    }

    /**
     * 检索相关FAQ
     */
    private List<Faq> searchRelatedFaqs(String question) {
        final String keyword = extractKeyword(question);
        final String searchKeyword = (keyword == null || keyword.isEmpty()) ? question : keyword;

        List<Faq> cachedFaqs = getCachedFaqs(searchKeyword);
        if (cachedFaqs != null) {
            return cachedFaqs;
        }

        return cacheService.executeWithLock(
                FAQ_CACHE_LOCK, 3, 10, TimeUnit.SECONDS,
                () -> {
                    List<Faq> reCheck = getCachedFaqs(searchKeyword);
                    if (reCheck != null) {
                        return reCheck;
                    }
                    // 这里简单处理，实际应该调用 FaqMapper
                    return java.util.Collections.emptyList();
                }
        );
    }

    private List<Faq> getCachedFaqs(String keyword) {
        String cacheKey = "im:ai:faq:search:" + keyword.hashCode();
        return cacheService.getCache(cacheKey);
    }

    private void setCachedFaqs(String keyword, List<Faq> faqs) {
        String cacheKey = "im:ai:faq:search:" + keyword.hashCode();
        cacheService.setCache(cacheKey, faqs);
    }

    private String extractKeyword(String question) {
        String[] words = question.split("[，。？！、\\s]+");
        if (words.length > 0 && !words[0].isEmpty()) {
            return words[0];
        }
        return question;
    }

    private String buildPrompt(String question, List<Faq> faqs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能客服，请根据以下知识库回答用户问题。\n\n");

        if (faqs != null && !faqs.isEmpty()) {
            sb.append("【知识库】\n");
            for (Faq faq : faqs) {
                sb.append("Q: ").append(faq.getQuestion()).append("\n");
                sb.append("A: ").append(faq.getAnswer()).append("\n\n");
            }
        }

        sb.append("【用户问题】\n").append(question).append("\n\n");
        sb.append("请用简洁友好的语气回答。如果知识库中有相关内容，优先使用知识库中的答案；")
                .append("如果没有找到相关信息，请说\"这个问题我暂时无法解答，建议您联系人工客服\"。")
                .append("回答不要过长，尽量控制在100字以内。");

        return sb.toString();
    }

    /**
     * 获取用户的会话列表
     */
    @SuppressWarnings("unchecked")
    public List<AiConversation> getConversationList(Long userId) {
        String cacheKey = CONVERSATION_LIST_CACHE + userId;
        try {
            List<AiConversation> cached = cacheService.getCache(cacheKey);
            if (cached != null) {
                log.debug("会话列表缓存命中: {}", cacheKey);
                return cached;
            }
        } catch (Exception e) {
            log.warn("获取会话列表缓存失败: {}", cacheKey, e);
        }

        List<AiConversation> list = conversationMapper.selectByUserId(userId);

        try {
            cacheService.setCache(cacheKey, list);
            log.debug("会话列表已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("设置会话列表缓存失败: {}", cacheKey, e);
        }

        return list;
    }

    /**
     * 获取会话详情
     */
    public AiConversation getConversation(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 获取会话消息列表
     */
    @SuppressWarnings("unchecked")
    public List<AiChatMessage> getConversationMessages(Long conversationId) {
        String cacheKey = CONVERSATION_MESSAGES_CACHE + conversationId;
        try {
            List<AiChatMessage> cached = cacheService.getCache(cacheKey);
            if (cached != null) {
                log.debug("会话消息缓存命中: {}", cacheKey);
                return cached;
            }
        } catch (Exception e) {
            log.warn("获取会话消息缓存失败: {}", cacheKey, e);
        }

        List<AiChatMessage> messages = messageMapper.selectByConversationId(conversationId);

        try {
            cacheService.setCache(cacheKey, messages);
            log.debug("会话消息已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("设置会话消息缓存失败: {}", cacheKey, e);
        }

        return messages;
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteConversation(Long conversationId) {
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conversationMapper.deleteById(conversationId);
            // 清除相关缓存
            clearConversationMessagesCache(conversationId);
            clearConversationListCache(conv.getUserId());
        }
    }

    // ========== 缓存相关方法 ==========

    private void clearConversationListCache(Long userId) {
        String cacheKey = CONVERSATION_LIST_CACHE + userId;
        try {
            cacheService.deleteCache(cacheKey);
            log.debug("会话列表缓存已清除: {}", cacheKey);
        } catch (Exception e) {
            log.warn("清除会话列表缓存失败: {}", cacheKey, e);
        }
    }

    private void clearConversationMessagesCache(Long conversationId) {
        String cacheKey = CONVERSATION_MESSAGES_CACHE + conversationId;
        try {
            cacheService.deleteCache(cacheKey);
            log.debug("会话消息缓存已清除: {}", cacheKey);
        } catch (Exception e) {
            log.warn("清除会话消息缓存失败: {}", cacheKey, e);
        }
    }
}
