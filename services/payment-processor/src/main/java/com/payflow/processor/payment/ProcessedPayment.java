package com.payflow.processor.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_payments")
public class ProcessedPayment {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedPayment() {
    }

    public ProcessedPayment(UUID paymentId) {
        this.paymentId = paymentId;
    }

    @PrePersist
    void setProcessedAt() {
        processedAt = Instant.now();
    }
}
