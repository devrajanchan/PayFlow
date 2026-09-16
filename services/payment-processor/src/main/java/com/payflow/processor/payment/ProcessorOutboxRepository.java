package com.payflow.processor.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessorOutboxRepository extends JpaRepository<ProcessorOutbox, UUID> {
    List<ProcessorOutbox> findByStatusOrderByCreatedAtAsc(ProcessorOutbox.Status status);
}
