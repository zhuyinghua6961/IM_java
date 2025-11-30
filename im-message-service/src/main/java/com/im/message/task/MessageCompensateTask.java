package com.im.message.task;

import com.alibaba.fastjson2.JSON;
import com.im.message.constant.RedisKeyConstant;
import com.im.message.entity.Message;
import com.im.message.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 消息补偿任务
 * 定时检查PENDING状态超时的消息，重新发送到Kafka
 */
@Slf4j
@Component
public class MessageCompensateTask {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${im.message.kafka-topic}")
    private String kafkaTopic;
    
    /**
     * 补偿PENDING消息
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void compensatePendingMessages() {
        log.debug("开始执行消息补偿任务");
        
        try {
            // 扫描Redis中所有消息详情
            Set<String> keys = redisTemplate.keys(RedisKeyConstant.MESSAGE_DETAIL_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.debug("没有找到待补偿的消息");
                return;
            }
            
            int compensateCount = 0;
            long now = System.currentTimeMillis();
            
            for (String key : keys) {
                try {
                    String msgJson = redisTemplate.opsForValue().get(key);
                    if (msgJson == null) {
                        continue;
                    }
                    
                    Message msg = JSON.parseObject(msgJson, Message.class);
                    
                    // 检查条件：PENDING状态 + 超过5分钟
                    if (!RedisKeyConstant.PERSIST_STATUS_PENDING.equals(msg.getPersistStatus())) {
                        continue;
                    }
                    
                    // 检查发送时间
                    if (msg.getSendTime() == null) {
                        continue;
                    }
                    
                    long sendTimeMillis = msg.getSendTime().atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                    
                    if (now - sendTimeMillis < 5 * 60 * 1000) {
                        // 未超时，跳过
                        continue;
                    }
                    
                    // 检查MySQL是否已存在
                    Message dbMsg = messageMapper.selectById(msg.getId());
                    if (dbMsg != null) {
                        // 数据库已有，更新Redis状态
                        msg.setPersistStatus(RedisKeyConstant.PERSIST_STATUS_PERSISTED);
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(msg));
                        log.info("补偿任务：消息已在数据库，更新状态: {}", msg.getId());
                        continue;
                    }
                    
                    // 检查是否被撤回
                    String cancelKey = RedisKeyConstant.getMessageCancelKey(msg.getId());
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(cancelKey))) {
                        // 已撤回，删除Redis
                        redisTemplate.delete(key);
                        log.info("补偿任务：消息已撤回，清理Redis: {}", msg.getId());
                        continue;
                    }
                    
                    // 数据库没有，重新发送到Kafka
                    kafkaTemplate.send(kafkaTopic, JSON.toJSONString(Map.of(
                        "action", "PERSIST",
                        "data", msg
                    )));
                    
                    log.warn("补偿任务：发现未持久化消息，重新发送: messageId={}, from={}, to={}", 
                        msg.getId(), msg.getFromUserId(), msg.getToId());
                    compensateCount++;
                    
                } catch (Exception e) {
                    log.error("补偿单条消息失败: key={}", key, e);
                }
            }
            
            if (compensateCount > 0) {
                log.warn("补偿任务完成，重新发送消息数: {}", compensateCount);
            } else {
                log.debug("补偿任务完成，没有需要补偿的消息");
            }
            
        } catch (Exception e) {
            log.error("消息补偿任务执行失败", e);
        }
    }
}
