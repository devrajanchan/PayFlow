package com.payflow.intake.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false, length = 100)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 100)
    private String destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(String clientId, String idempotencyKey, String sourceAccountId,
                   String destinationAccountId, BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.RECEIVED;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getClientId() { return clientId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
