package com.payflow.intake.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentOutboxService {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    public PaymentOutboxService(PaymentOutboxRepository paymentOutboxRepository,
                               PaymentEventPublisher paymentEventPublisher,
                               ObjectMapper objectMapper) {
        this.paymentOutboxRepository = paymentOutboxRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordAccepted(Payment payment) {
        PaymentAcceptedEvent event = PaymentAcceptedEvent.from(payment);
        String payload = toJson(event);
        paymentOutboxRepository.save(new PaymentOutbox(
            payment.getId(),
            "Payment",
            "payment.accepted",
            payload
        ));
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<PaymentOutbox> pending = paymentOutboxRepository.findByStatusOrderByCreatedAtAsc(
                PaymentOutbox.OutboxStatus.PENDING);

        for (PaymentOutbox outbox : pending) {
            PaymentAcceptedEvent event = readPayload(outbox.getPayload());
            paymentEventPublisher.publishAccepted(event);
            outbox.markPublished();
            paymentOutboxRepository.save(outbox);
        }
    }

    private String toJson(PaymentAcceptedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment accepted event", e);
        }
    }

    private PaymentAcceptedEvent readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentAcceptedEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize payment accepted event", e);
        }
    }
}
