package com.im.message.service.impl;

import com.alibaba.fastjson2.JSON;
import com.im.message.constant.RedisKeyConstant;
import com.im.message.entity.Message;
import com.im.message.mapper.MessageDeleteMapper;
import com.im.message.service.MessageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消息缓存服务实现
 */
@Slf4j
@Service
public class MessageCacheServiceImpl implements MessageCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private MessageDeleteMapper messageDeleteMapper;
    
    @Value("${im.message.kafka-topic}")
    private String kafkaTopic;
    
    @Value("${im.message.kafka-partitions:10}")
    private Integer kafkaPartitions;
    
    @Value("${im.message.conv-cache-size:1000}")
    private Integer convCacheSize;
    
    @Value("${im.message.cache-expire-minutes:30}")
    private Integer cacheExpireMinutes;
    
    @Override
    public void cacheAndSendToKafka(Message message) {
        String messageId = String.valueOf(message.getId());
        
        // 1. 缓存单条消息详情
        String detailKey = RedisKeyConstant.getMessageDetailKey(message.getId());
        redisTemplate.opsForValue().set(
            detailKey,
            JSON.toJSONString(message),
            cacheExpireMinutes,
            TimeUnit.MINUTES
        );
        log.debug("缓存消息详情: messageId={}", messageId);
        
        // 2. 添加到会话消息列表（只针对单聊）
        if (message.getChatType() == 1) {
            String conversationId = generateConversationId(message.getFromUserId(), message.getToId());
            String convKey = RedisKeyConstant.getConversationMessagesKey(conversationId);
            
            // 左侧插入（最新消息在前）
            redisTemplate.opsForList().leftPush(convKey, JSON.toJSONString(message));
            // 只保留最近N条
            redisTemplate.opsForList().trim(convKey, 0, convCacheSize - 1);
            // 设置过期时间
            redisTemplate.expire(convKey, cacheExpireMinutes, TimeUnit.MINUTES);
            
            log.debug("缓存会话消息: conversationId={}, messageId={}", conversationId, messageId);
        }
        
        // 3. 发送到Kafka异步持久化
        try {
            String conversationId = message.getChatType() == 1 ? 
                generateConversationId(message.getFromUserId(), message.getToId()) :
                String.valueOf(message.getToId());
            
            // 按会话ID分区，保证同一会话消息有序
            int partition = Math.abs(conversationId.hashCode()) % kafkaPartitions;
            
            kafkaTemplate.send(
                kafkaTopic,
                partition,
                conversationId,
                JSON.toJSONString(Map.of(
                    "action", "PERSIST",
                    "data", message
                ))
            );
            
            log.info("消息已缓存并发送到Kafka: messageId={}, partition={}", messageId, partition);
        } catch (Exception e) {
            // Kafka发送失败，但Redis已缓存，依赖补偿任务持久化
            log.warn("发送消息到Kafka失败，将由补偿任务处理: messageId={}, error={}", 
                messageId, e.getMessage());
        }
    }
    
    @Override
    public List<Message> getConversationMessagesFromCache(String conversationId, int limit) {
        String convKey = RedisKeyConstant.getConversationMessagesKey(conversationId);
        
        // 从Redis获取最近的消息
        List<String> messageJsonList = redisTemplate.opsForList().range(convKey, 0, limit - 1);
        
        if (messageJsonList == null || messageJsonList.isEmpty()) {
            log.debug("Redis中没有会话消息缓存: conversationId={}", conversationId);
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        for (String msgJson : messageJsonList) {
            try {
                Message msg = JSON.parseObject(msgJson, Message.class);
                // 只返回正常状态的消息，过滤掉已撤回的消息（status=0）
                if (msg.getStatus() != null && msg.getStatus() == 1) {
                    messages.add(msg);
                }
            } catch (Exception e) {
                log.error("解析消息JSON失败: {}", msgJson, e);
            }
        }
        
        log.debug("从Redis获取会话消息（过滤后）: conversationId={}, count={}", conversationId, messages.size());
        return messages;
    }
    
    @Override
    public Message getMessageFromCache(Long messageId) {
        String detailKey = RedisKeyConstant.getMessageDetailKey(messageId);
        log.info("从Redis查询消息详情: key={}", detailKey);
        String msgJson = redisTemplate.opsForValue().get(detailKey);
        
        if (msgJson == null) {
            log.warn("Redis中没有消息详情: messageId={}, key={}", messageId, detailKey);
            return null;
        }
        
        try {
            Message msg = JSON.parseObject(msgJson, Message.class);
            log.info("Redis消息详情: messageId={}, status={}, persistStatus={}", 
                messageId, msg.getStatus(), msg.getPersistStatus());
            return msg;
        } catch (Exception e) {
            log.error("解析消息JSON失败: messageId={}, json={}", messageId, msgJson, e);
            return null;
        }
    }
    
    @Override
    public void markMessageAsRecalled(Long messageId, String persistStatus) {
        if (RedisKeyConstant.PERSIST_STATUS_PENDING.equals(persistStatus)) {
            // 消息还在队列中，设置撤回标记
            String cancelKey = RedisKeyConstant.getMessageCancelKey(messageId);
            redisTemplate.opsForValue().set(cancelKey, "1", 10, TimeUnit.MINUTES);
            log.info("设置消息撤回标记: messageId={}", messageId);
            
        } else if (RedisKeyConstant.PERSIST_STATUS_PERSISTED.equals(persistStatus)) {
            // 消息已持久化，发送撤回事件到Kafka
            kafkaTemplate.send(kafkaTopic, JSON.toJSONString(Map.of(
                "action", "RECALL",
                "messageId", messageId,
                "recallTime", System.currentTimeMillis()
            )));
            log.info("发送消息撤回事件到Kafka: messageId={}", messageId);
        }
        
        // 更新Redis中的消息状态
        Message cachedMsg = getMessageFromCache(messageId);
        if (cachedMsg != null) {
            cachedMsg.setStatus(0); // 已撤回
            cachedMsg.setRecallTime(java.time.LocalDateTime.now());
            
            // 1. 更新消息详情key
            String detailKey = RedisKeyConstant.getMessageDetailKey(messageId);
            redisTemplate.opsForValue().set(
                detailKey,
                JSON.toJSONString(cachedMsg),
                cacheExpireMinutes,
                TimeUnit.MINUTES
            );
            
            // 2. 更新会话列表中的消息（单聊消息才有会话列表）
            if (cachedMsg.getChatType() == 1) {
                String conversationId = generateConversationId(cachedMsg.getFromUserId(), cachedMsg.getToId());
                String convKey = RedisKeyConstant.getConversationMessagesKey(conversationId);
                
                // 获取会话列表
                List<String> messageJsonList = redisTemplate.opsForList().range(convKey, 0, -1);
                if (messageJsonList != null && !messageJsonList.isEmpty()) {
                    // 找到并替换该消息
                    for (int i = 0; i < messageJsonList.size(); i++) {
                        try {
                            Message msg = JSON.parseObject(messageJsonList.get(i), Message.class);
                            if (msg.getId().equals(messageId)) {
                                // 找到了，更新这条消息
                                redisTemplate.opsForList().set(convKey, i, JSON.toJSONString(cachedMsg));
                                log.info("已更新会话列表中的撤回消息: conversationId={}, messageId={}", conversationId, messageId);
                                break;
                            }
                        } catch (Exception e) {
                            log.error("解析会话消息JSON失败", e);
                        }
                    }
                }
            }
            
            log.info("消息撤回状态已更新到Redis: messageId={}", messageId);
        }
    }
    
    @Override
    public boolean isMessageDeleted(Long userId, Long messageId) {
        // 先查Redis
        String deletedKey = RedisKeyConstant.getUserDeletedMessagesKey(userId);
        Boolean isMember = redisTemplate.opsForSet().isMember(deletedKey, messageId.toString());
        
        if (Boolean.TRUE.equals(isMember)) {
            return true;
        }
        
        // Redis没有，查数据库
        com.im.message.entity.MessageDelete delete = 
            messageDeleteMapper.selectByUserAndMessage(userId, messageId);
        
        if (delete != null) {
            // 写入Redis缓存
            redisTemplate.opsForSet().add(deletedKey, messageId.toString());
            redisTemplate.expire(deletedKey, cacheExpireMinutes, TimeUnit.MINUTES);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void markMessageAsDeleted(Long userId, Long messageId) {
        String deletedKey = RedisKeyConstant.getUserDeletedMessagesKey(userId);
        redisTemplate.opsForSet().add(deletedKey, messageId.toString());
        redisTemplate.expire(deletedKey, cacheExpireMinutes, TimeUnit.MINUTES);
        log.debug("标记消息已删除（Redis）: userId={}, messageId={}", userId, messageId);
    }
    
    @Override
    public String generateConversationId(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }
}
