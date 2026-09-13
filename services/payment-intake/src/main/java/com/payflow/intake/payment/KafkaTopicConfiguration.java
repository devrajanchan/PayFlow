package com.payflow.intake.payment;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    NewTopic paymentAcceptedTopic() {
        return TopicBuilder.name(KafkaPaymentEventPublisher.PAYMENT_ACCEPTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
