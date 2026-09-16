package com.payflow.processor.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPayment, UUID> {
}
