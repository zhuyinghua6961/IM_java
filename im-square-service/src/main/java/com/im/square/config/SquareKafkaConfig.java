package com.im.square.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SquareKafkaConfig {

    @Value("${im.square.feed-kafka-topic:im-square-feed}")
    private String feedKafkaTopic;

    @Value("${im.square.feed-kafka-partitions:3}")
    private int feedKafkaPartitions;

    @Bean
    public NewTopic squareFeedTopic() {
        // 直接使用 NewTopic 构造函数，避免依赖 TopicBuilder（不同 Spring Kafka 版本可能不存在）
        return new NewTopic(feedKafkaTopic, feedKafkaPartitions, (short) 1);
    }
}
