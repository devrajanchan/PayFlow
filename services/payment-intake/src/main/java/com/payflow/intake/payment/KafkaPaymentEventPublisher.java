package com.payflow.intake.payment;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    static final String PAYMENT_ACCEPTED_TOPIC = "payment.accepted";

    private final KafkaTemplate<String, PaymentAcceptedEvent> kafkaTemplate;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, PaymentAcceptedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishAccepted(PaymentAcceptedEvent event) {
        kafkaTemplate.send(PAYMENT_ACCEPTED_TOPIC, event.paymentId().toString(), event);
    }
}
