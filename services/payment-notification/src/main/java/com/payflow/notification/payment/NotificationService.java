package com.payflow.notification.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRecordRepository notificationRecordRepository;

    public NotificationService(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Transactional
    public void notifyPaymentProcessed(PaymentProcessedEvent event) {
        if (notificationRecordRepository.existsById(event.paymentId())) {
            return;
        }

        notificationRecordRepository.save(new NotificationRecord(
                event.paymentId(), "IN_APP", "SENT"));
    }
}
