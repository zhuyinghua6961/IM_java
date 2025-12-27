package com.im.ai.service;

import com.im.ai.entity.AiChatHistory;
import com.im.ai.entity.Faq;
import com.im.ai.mapper.AiChatHistoryMapper;
import com.im.ai.mapper.FaqMapper;
import com.im.ai.util.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI客服服务
 * 实现二级缓存：L1 本地缓存 + L2 Redis 分布式缓存
 */
@Slf4j
@Service
public class AiService {

    @Autowired
    private FaqMapper faqMapper;

    @Autowired
    private AiChatHistoryMapper aiChatHistoryMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private CacheService cacheService;

    /**
     * 客服账号ID（固定为0表示官方客服）
     */
    public static final long CUSTOMER_SERVICE_ID = 0L;

    /**
     * 缓存锁名称
     */
    private static final String FAQ_CACHE_LOCK = "im:ai:faq:lock";

    /**
     * 生成客服回复
     */
    public String generateReply(String userQuestion) {
        try {
            // 1. 检索知识库，获取相关FAQ
            List<Faq> relatedFaqs = searchRelatedFaqs(userQuestion);

            // 2. 构建提示词
            String prompt = buildPrompt(userQuestion, relatedFaqs);

            // 3. 调用LLM
            String answer = deepSeekClient.chat(userQuestion, prompt);

            return answer;
        } catch (Exception e) {
            log.error("生成AI回复失败", e);
            return "抱歉，我现在有点忙，请稍后再试。";
        }
    }

    /**
     * 简单回复（不检索知识库）
     */
    public String simpleChat(String message) {
        return deepSeekClient.chat(message, "你是一个智能客服，请用简洁友好的语气回答用户问题，回答控制在100字以内。");
    }

    /**
     * 检索相关FAQ - 二级缓存实现
     */
    private List<Faq> searchRelatedFaqs(String question) {
        // 简单关键词匹配：提取问题中的关键词
        final String keyword = extractKeyword(question);
        final String searchKeyword = (keyword == null || keyword.isEmpty()) ? question : keyword;

        // 先从缓存获取
        List<Faq> cachedFaqs = getCachedFaqs(searchKeyword);
        if (cachedFaqs != null) {
            return cachedFaqs;
        }

        // 缓存未命中，获取分布式锁后查询数据库
        return cacheService.executeWithLock(
                FAQ_CACHE_LOCK,
                3,
                10,
                TimeUnit.SECONDS,
                () -> {
                    // 双重检查缓存
                    List<Faq> reCheck = getCachedFaqs(searchKeyword);
                    if (reCheck != null) {
                        return reCheck;
                    }

                    // 查询数据库
                    List<Faq> faqs = faqMapper.searchByKeyword(searchKeyword, 5);

                    // 存入缓存
                    setCachedFaqs(searchKeyword, faqs);

                    return faqs;
                }
        );
    }

    /**
     * 从缓存获取FAQ列表
     */
    @SuppressWarnings("unchecked")
    private List<Faq> getCachedFaqs(String keyword) {
        String cacheKey = buildCacheKey(keyword);
        return cacheService.getCache(cacheKey);
    }

    /**
     * 设置FAQ缓存
     */
    private void setCachedFaqs(String keyword, List<Faq> faqs) {
        String cacheKey = buildCacheKey(keyword);
        cacheService.setCache(cacheKey, faqs);
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(String keyword) {
        return "im:ai:faq:search:" + keyword.hashCode();
    }

    /**
     * 从问题中提取关键词
     */
    private String extractKeyword(String question) {
        // 简单处理：取第一个词作为关键词
        String[] words = question.split("[，。？！、\\s]+");
        if (words.length > 0 && !words[0].isEmpty()) {
            return words[0];
        }
        return question;
    }

    /**
     * 构建提示词
     */
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

        sb.append("【用户问题】\n");
        sb.append(question).append("\n\n");

        sb.append("请用简洁友好的语气回答。如果知识库中有相关内容，优先使用知识库中的答案；")
                .append("如果没有找到相关信息，请说\"这个问题我暂时无法解答，建议您联系人工客服\"。")
                .append("回答不要过长，尽量控制在100字以内。");

        return sb.toString();
    }

    /**
     * 清除所有FAQ缓存
     */
    public void clearFaqCache() {
        cacheService.deleteAllFaqCache();
        log.info("FAQ缓存已清除");
    }

    /**
     * 获取缓存中FAQ数量（用于监控）
     */
    public int getCachedFaqCount() {
        // 这里简化处理，实际可以通过 Redis keys 统计
        return -1;
    }

    /**
     * 保存聊天记录
     */
    public void saveChatHistory(Long userId, String message, String reply, String category) {
        try {
            AiChatHistory history = new AiChatHistory();
            history.setUserId(userId);
            history.setMessage(message);
            history.setReply(reply);
            history.setCategory(category);
            aiChatHistoryMapper.insert(history);
            log.debug("聊天记录已保存: userId={}", userId);
        } catch (Exception e) {
            log.error("保存聊天记录失败", e);
        }
    }

    /**
     * 获取用户聊天记录
     */
    public List<AiChatHistory> getChatHistory(Long userId) {
        try {
            return aiChatHistoryMapper.selectByUserId(userId);
        } catch (Exception e) {
            log.error("获取聊天记录失败, userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户最近的聊天记录
     */
    public List<AiChatHistory> getRecentChatHistory(Long userId, int limit) {
        try {
            return aiChatHistoryMapper.selectRecentByUserId(userId, limit);
        } catch (Exception e) {
            log.error("获取最近聊天记录失败, userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 清空用户聊天记录
     */
    public void clearChatHistory(Long userId) {
        try {
            aiChatHistoryMapper.deleteByUserId(userId);
            log.info("用户聊天记录已清空: userId={}", userId);
        } catch (Exception e) {
            log.error("清空聊天记录失败, userId={}", userId, e);
        }
    }
}
