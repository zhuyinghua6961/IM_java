package com.im.message.kafka;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.im.message.constant.RedisKeyConstant;
import com.im.message.entity.Message;
import com.im.message.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 消息Kafka消费者
 * 负责异步持久化消息到MySQL
 */
@Slf4j
@Component
public class MessageKafkaConsumer {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Value("${im.message.cache-expire-minutes:30}")
    private Integer cacheExpireMinutes;
    
    /**
     * 消费消息
     */
    @KafkaListener(topics = "${im.message.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String messageValue = record.value();
            log.debug("收到Kafka消息: {}", messageValue);
            
            JSONObject json = JSON.parseObject(messageValue);
            String action = json.getString("action");
            
            if ("PERSIST".equals(action)) {
                // 持久化消息
                handlePersist(json);
            } else if ("RECALL".equals(action)) {
                // 撤回消息
                handleRecall(json);
            }
            
            // 手动确认消费成功
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("消费Kafka消息失败: {}", record.value(), e);
            // 确认消费，避免阻塞（已记录错误日志）
            ack.acknowledge();
        }
    }
    
    /**
     * 处理消息持久化
     */
    private void handlePersist(JSONObject json) {
        Message msg = json.getObject("data", Message.class);
        Long messageId = msg.getId();
        
        log.info("开始持久化消息: messageId={}, from={}, to={}", 
            messageId, msg.getFromUserId(), msg.getToId());
        
        // 第一次检查：撤回标记
        String cancelKey = RedisKeyConstant.getMessageCancelKey(messageId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cancelKey))) {
            log.info("消息已撤回，跳过持久化: {}", messageId);
            // 清理Redis详情
            redisTemplate.delete(RedisKeyConstant.getMessageDetailKey(messageId));
            return;
        }
        
        // 幂等性检查：获取分布式锁
        String lockKey = RedisKeyConstant.getMessageLockKey(messageId);
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("消息正在处理或已处理，跳过: {}", messageId);
            return;
        }
        
        try {
            // 第二次检查：撤回标记（写库前）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cancelKey))) {
                log.info("写库前检测到撤回，跳过: {}", messageId);
                return;
            }
            
            // 检查数据库是否已存在（防止重复插入）
            Message existMsg = messageMapper.selectById(messageId);
            if (existMsg != null) {
                log.warn("消息已存在数据库，跳过插入: {}", messageId);
                // 更新Redis状态
                updateRedisPersistStatus(messageId);
                return;
            }
            
            // 写入MySQL
            messageMapper.insert(msg);
            log.info("消息持久化成功: {}", messageId);
            
            // 第三次检查：撤回标记（写库后）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cancelKey))) {
                log.warn("写库后检测到撤回，立即删除: {}", messageId);
                // 立即删除刚插入的数据
                messageMapper.deleteById(messageId);
                redisTemplate.delete(RedisKeyConstant.getMessageDetailKey(messageId));
                return;
            }
            
            // 更新Redis中的持久化状态
            updateRedisPersistStatus(messageId);
            
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 处理消息撤回
     */
    private void handleRecall(JSONObject json) {
        Long messageId = json.getLong("messageId");
        Long recallTime = json.getLong("recallTime");
        
        log.info("处理消息撤回: messageId={}, recallTime={}", messageId, recallTime);
        
        // 更新数据库
        Message update = new Message();
        update.setId(messageId);
        update.setStatus(0); // 已撤回
        update.setRecallTime(LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(recallTime),
            java.time.ZoneId.systemDefault()
        ));
        
        int rows = messageMapper.updateById(update);
        if (rows > 0) {
            log.info("消息撤回成功（数据库）: {}", messageId);
        } else {
            log.warn("消息撤回失败，消息不存在: {}", messageId);
        }
    }
    
    /**
     * 更新Redis中的持久化状态
     */
    private void updateRedisPersistStatus(Long messageId) {
        String detailKey = RedisKeyConstant.getMessageDetailKey(messageId);
        String msgJson = redisTemplate.opsForValue().get(detailKey);
        
        if (msgJson != null) {
            Message cachedMsg = JSON.parseObject(msgJson, Message.class);
            cachedMsg.setPersistStatus(RedisKeyConstant.PERSIST_STATUS_PERSISTED);
            redisTemplate.opsForValue().set(
                detailKey, 
                JSON.toJSONString(cachedMsg), 
                cacheExpireMinutes, 
                TimeUnit.MINUTES
            );
            log.debug("更新Redis持久化状态: messageId={}", messageId);
        }
    }
}
