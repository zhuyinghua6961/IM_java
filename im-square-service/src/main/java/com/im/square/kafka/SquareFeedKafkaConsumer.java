package com.im.square.kafka;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.im.square.entity.SquarePost;
import com.im.square.mapper.SquareFollowMapper;
import com.im.square.mapper.SquarePostMapper;
import com.im.square.service.RemoteSquareNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 广场Feed流Kafka消费者
 * 负责根据发帖事件，把帖子ID异步推送到粉丝的Redis Feed中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SquareFeedKafkaConsumer {

    private final SquareFollowMapper followMapper;
    private final StringRedisTemplate redisTemplate;
    private final SquarePostMapper postMapper;
    private final RemoteSquareNotificationService remoteSquareNotificationService;

    private static final String FOLLOW_FEED_KEY_PREFIX = "square:feed:user:";
    private static final long FOLLOW_FEED_MAX_SIZE = 500L;

    @KafkaListener(topics = "${im.square.feed-kafka-topic:im-square-feed}", groupId = "im-square-feed-consumer")
    public void consumeFeedEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String value = record.value();
            log.debug("收到广场Feed Kafka消息: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), value);

            if (value == null || value.isEmpty()) {
                ack.acknowledge();
                return;
            }

            JSONObject json = JSON.parseObject(value);
            String postIdStr = json.getString("postId");
            String authorIdStr = json.getString("authorId");
            long createTimeMillis = json.getLongValue("createTime");

            if (postIdStr == null || authorIdStr == null) {
                log.warn("广场Feed消息缺少必要字段: {}", value);
                ack.acknowledge();
                return;
            }

            Long authorId;
            Long postId;
            try {
                authorId = Long.valueOf(authorIdStr);
                postId = Long.valueOf(postIdStr);
            } catch (NumberFormatException e) {
                log.warn("解析authorId或postId失败: authorIdStr={}, postIdStr={}", authorIdStr, postIdStr, e);
                ack.acknowledge();
                return;
            }

            SquarePost post = postMapper.selectById(postId);
            if (post == null) {
                log.warn("广场Feed事件对应的帖子不存在, postId={}", postIdStr);
                ack.acknowledge();
                return;
            }

            // 根据作者ID查询所有粉丝
            List<Long> followerIds = followMapper.selectFollowerIds(authorId);
            if (followerIds == null || followerIds.isEmpty()) {
                log.info("广场Feed事件无粉丝需要推送, authorId={}, postId={}", authorId, postIdStr);
                ack.acknowledge();
                return;
            }

            log.info("消费广场Feed事件, authorId={}, postId={}, followerCount={}", authorId, postIdStr, followerIds.size());

            double score = (double) createTimeMillis;
            for (Long followerId : followerIds) {
                if (followerId == null) continue;
                String feedKey = FOLLOW_FEED_KEY_PREFIX + followerId;
                try {
                    // 使用字符串类型存储postId，避免雪花ID精度问题
                    redisTemplate.opsForZSet().add(feedKey, postIdStr, score);
                    Long size = redisTemplate.opsForZSet().size(feedKey);
                    if (size != null && size > FOLLOW_FEED_MAX_SIZE) {
                        long removeCount = size - FOLLOW_FEED_MAX_SIZE;
                        if (removeCount > 0) {
                            redisTemplate.opsForZSet().removeRange(feedKey, 0, removeCount - 1);
                        }
                    }
                    remoteSquareNotificationService.sendFollowPostNotification(post, followerId);
                } catch (Exception e) {
                    log.warn("写入粉丝Feed失败, followerId={}, postId={}", followerId, postIdStr, e);
                }
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("消费广场Feed Kafka消息失败: {}", record.value(), e);
            // 仍然确认，避免阻塞，异常通过日志观察
            ack.acknowledge();
        }
    }
}
