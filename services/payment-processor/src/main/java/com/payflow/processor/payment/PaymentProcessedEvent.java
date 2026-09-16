package com.payflow.processor.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        Instant processedAt
) {
    public static PaymentProcessedEvent from(PaymentAcceptedEvent event) {
        return new PaymentProcessedEvent(event.paymentId(), event.amount(),
                event.currency(), Instant.now());
    }
}
