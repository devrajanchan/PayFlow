package com.payflow.processor.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessorOutboxService {

    static final String PAYMENT_PROCESSED_TOPIC = "payment.processed";

    private final ProcessorOutboxRepository outboxRepository;
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ProcessorOutboxService(ProcessorOutboxRepository outboxRepository,
                                  KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordProcessed(PaymentAcceptedEvent event) {
        outboxRepository.save(new ProcessorOutbox(event.paymentId(), PAYMENT_PROCESSED_TOPIC,
                toJson(PaymentProcessedEvent.from(event))));
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<ProcessorOutbox> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
                ProcessorOutbox.Status.PENDING);
        for (ProcessorOutbox outbox : pending) {
            PaymentProcessedEvent event = fromJson(outbox.getPayload());
            kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, event.paymentId().toString(), event);
            outbox.markPublished();
            outboxRepository.save(outbox);
        }
    }

    private String toJson(PaymentProcessedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment processed event", e);
        }
    }

    private PaymentProcessedEvent fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentProcessedEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize payment processed event", e);
        }
    }
}
