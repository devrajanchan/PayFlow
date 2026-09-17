package com.payflow.notification.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRecordRepository notificationRecordRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void recordsOneNotificationForProcessedPayment() {
        UUID paymentId = UUID.randomUUID();
        PaymentProcessedEvent event = event(paymentId);
        when(notificationRecordRepository.existsById(paymentId)).thenReturn(false);

        notificationService.notifyPaymentProcessed(event);

        verify(notificationRecordRepository).save(org.mockito.ArgumentMatchers.argThat(record ->
                record.getPaymentId().equals(paymentId)
                        && record.getChannel().equals("IN_APP")
                        && record.getStatus().equals("SENT")));
    }

    @Test
    void ignoresDuplicateProcessedEvent() {
        UUID paymentId = UUID.randomUUID();
        when(notificationRecordRepository.existsById(paymentId)).thenReturn(true);

        notificationService.notifyPaymentProcessed(event(paymentId));

        verify(notificationRecordRepository, never())
                .save(org.mockito.ArgumentMatchers.any(NotificationRecord.class));
    }

    private PaymentProcessedEvent event(UUID paymentId) {
        return new PaymentProcessedEvent(paymentId, new BigDecimal("25.00"),
                "USD", Instant.now());
    }
}
