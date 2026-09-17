package com.payflow.notification.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, UUID> {
}
