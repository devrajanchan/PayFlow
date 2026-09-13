package com.payflow.intake.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

        @Mock
        private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void returnsExistingPaymentForRepeatedIdempotencyKey() {
        PaymentRequest request = new PaymentRequest("account-1", "account-2",
                new BigDecimal("25.00"), "USD");
        Payment existing = new Payment("client-1", "request-123", "account-1",
                "account-2", new BigDecimal("25.00"), "USD");
        UUID paymentId = existing.getId();

        when(paymentRepository.findByClientIdAndIdempotencyKey("client-1", "request-123"))
                .thenReturn(Optional.of(existing));

        Payment result = paymentService.create("client-1", "request-123", request);

        assertThat(result.getId()).isEqualTo(paymentId);
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any(Payment.class));
        verifyNoInteractions(paymentEventPublisher);
    }

    @Test
    void publishesAcceptedEventForNewPayment() {
        PaymentRequest request = new PaymentRequest("account-1", "account-2",
                new BigDecimal("25.00"), "USD");
        Payment saved = new Payment("client-1", "request-124", "account-1",
                "account-2", new BigDecimal("25.00"), "USD");

        when(paymentRepository.findByClientIdAndIdempotencyKey("client-1", "request-124"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class)))
                .thenReturn(saved);

        Payment result = paymentService.create("client-1", "request-124", request);

        assertThat(result).isSameAs(saved);
        verify(paymentEventPublisher).publishAccepted(org.mockito.ArgumentMatchers.argThat(event ->
                event.paymentId().equals(saved.getId())
                        && event.amount().compareTo(new BigDecimal("25.00")) == 0
                        && event.currency().equals("USD")));
    }
}
