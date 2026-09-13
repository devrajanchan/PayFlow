package com.payflow.intake.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxService paymentOutboxService;

    public PaymentService(PaymentRepository paymentRepository, PaymentOutboxService paymentOutboxService) {
        this.paymentRepository = paymentRepository;
        this.paymentOutboxService = paymentOutboxService;
    }

    @Transactional
    public Payment create(String clientId, String idempotencyKey, PaymentRequest request) {
        return paymentRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
            .orElseGet(() -> createAndRecordOutbox(clientId, idempotencyKey, request));
    }

    private Payment createAndRecordOutbox(String clientId, String idempotencyKey, PaymentRequest request) {
        Payment payment = paymentRepository.save(new Payment(clientId, idempotencyKey,
            request.sourceAccountId(), request.destinationAccountId(),
            request.amount(), request.currency()));
        paymentOutboxService.recordAccepted(payment);
        return payment;
    }
}
