package com.payflow.processor.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    private final PaymentProcessingService paymentProcessingService;

    public PaymentProcessor(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @KafkaListener(topics = "payment.accepted", groupId = "payment-processor")
    public void onPaymentAccepted(PaymentAcceptedEvent event) {
        log.info("Processing payment accepted event: paymentId={}, amount={}, currency={}",
                event.paymentId(), event.amount(), event.currency());
        paymentProcessingService.process(event);
    }
}
