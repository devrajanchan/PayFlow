package com.payflow.intake.payment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
    List<PaymentOutbox> findByStatusOrderByCreatedAtAsc(PaymentOutbox.OutboxStatus status);
}
