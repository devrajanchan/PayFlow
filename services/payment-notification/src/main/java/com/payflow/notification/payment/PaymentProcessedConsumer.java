package com.payflow.notification.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessedConsumer.class);

    private final NotificationService notificationService;

    public PaymentProcessedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "payment.processed", groupId = "payment-notification")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Creating payment notification: paymentId={}, amount={}, currency={}",
                event.paymentId(), event.amount(), event.currency());
        notificationService.notifyPaymentProcessed(event);
    }
}
