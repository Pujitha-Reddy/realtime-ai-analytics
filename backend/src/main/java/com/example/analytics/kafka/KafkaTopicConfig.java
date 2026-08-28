package com.example.analytics.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("dev")
public class KafkaTopicConfig {
    @Value("${app.kafka.topic}")
    private String topicName;

    @Bean
    public NewTopic operationalEventsTopic() {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }
}
