package com.payflow.intake.payment;

public interface PaymentEventPublisher {
    void publishAccepted(PaymentAcceptedEvent event);
}
