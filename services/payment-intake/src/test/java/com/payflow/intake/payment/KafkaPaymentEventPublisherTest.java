package com.payflow.intake.payment;

import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
class KafkaPaymentEventPublisherTest {

    private MockProducer<String, PaymentAcceptedEvent> mockProducer;

    @Test
    void publishesPaymentIdAsKafkaKey() {
        mockProducer = new MockProducer<>(true, new StringSerializer(),
            new org.springframework.kafka.support.serializer.JsonSerializer<>());
        DefaultKafkaProducerFactory<String, PaymentAcceptedEvent> producerFactory =
                new DefaultKafkaProducerFactory<String, PaymentAcceptedEvent>(Map.of()) {
                    @Override
                    public Producer<String, PaymentAcceptedEvent> createProducer() {
                        return mockProducer;
                    }
                };
        KafkaTemplate<String, PaymentAcceptedEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafkaTemplate);
        UUID paymentId = UUID.randomUUID();
        PaymentAcceptedEvent event = new PaymentAcceptedEvent(paymentId, "client-1",
                "account-1", "account-2", new BigDecimal("25.00"), "USD", Instant.now());

        publisher.publishAccepted(event);

        ProducerRecord<String, PaymentAcceptedEvent> record = mockProducer.history().get(0);

        assertThat(record.topic()).isEqualTo("payment.accepted");
        assertThat(record.key()).isEqualTo(paymentId.toString());
        assertThat(record.value()).isSameAs(event);
    }

    @AfterEach
    void closeProducer() {
        if (mockProducer != null) {
            mockProducer.close();
        }
    }
}
