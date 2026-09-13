package com.payflow.intake.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getSourceAccountId(),
                payment.getDestinationAccountId(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
