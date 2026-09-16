package com.payflow.processor.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationResult(
        UUID paymentId,
        boolean balanced,
        BigDecimal amount,
        String currency,
        String reason
) {
    public static ReconciliationResult balanced(UUID paymentId, BigDecimal amount, String currency) {
        return new ReconciliationResult(paymentId, true, amount, currency, "DEBIT_CREDIT_MATCH");
    }

    public static ReconciliationResult failed(UUID paymentId, String reason) {
        return new ReconciliationResult(paymentId, false, null, null, reason);
    }
}