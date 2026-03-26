package org.innowise.internship.paymentservice.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic paymentRequestsTopic(
            @Value("${app.kafka.topics.payment-requests.name}") String name,
            @Value("${app.kafka.topics.payment-requests.partitions}") int partitions,
            @Value("${app.kafka.topics.payment-requests.replicas}") int replicas) {

        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    @Bean
    public NewTopic paymentResultsTopic(
            @Value("${app.kafka.topics.payment-results.name}") String name,
            @Value("${app.kafka.topics.payment-results.partitions}") int partitions,
            @Value("${app.kafka.topics.payment-results.replicas}") int replicas) {

        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
