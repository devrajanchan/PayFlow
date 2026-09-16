package com.payflow.processor.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAcceptedEvent(
        UUID paymentId,
        String clientId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
