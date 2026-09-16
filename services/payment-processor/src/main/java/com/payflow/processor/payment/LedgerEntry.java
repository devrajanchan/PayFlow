package com.payflow.processor.payment;

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
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID paymentId, String accountId, EntryType type,
                       BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public UUID getPaymentId() { return paymentId; }
    public String getAccountId() { return accountId; }
    public EntryType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    public enum EntryType {
        DEBIT,
        CREDIT
    }
}
