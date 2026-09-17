package com.payflow.notification.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_records")
public class NotificationRecord {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(UUID paymentId, String channel, String status) {
        this.paymentId = paymentId;
        this.channel = channel;
        this.status = status;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public UUID getPaymentId() { return paymentId; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
}
