package com.payflow.notification.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        Instant processedAt
) {
}
