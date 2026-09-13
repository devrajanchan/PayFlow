package com.payflow.intake.payment;

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
    public static PaymentAcceptedEvent from(Payment payment) {
        return new PaymentAcceptedEvent(payment.getId(), payment.getClientId(),
                payment.getSourceAccountId(), payment.getDestinationAccountId(),
                payment.getAmount(), payment.getCurrency(), Instant.now());
    }
}
