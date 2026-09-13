package com.payflow.intake.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public Payment create(String clientId, String idempotencyKey, PaymentRequest request) {
        return paymentRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
            .orElseGet(() -> createAndPublish(clientId, idempotencyKey, request));
        }

        private Payment createAndPublish(String clientId, String idempotencyKey, PaymentRequest request) {
        Payment payment = paymentRepository.save(new Payment(clientId, idempotencyKey,
            request.sourceAccountId(), request.destinationAccountId(),
            request.amount(), request.currency()));
        paymentEventPublisher.publishAccepted(PaymentAcceptedEvent.from(payment));
        return payment;
    }
}
