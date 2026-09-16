package com.payflow.processor.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private ProcessedPaymentRepository processedPaymentRepository;

    @InjectMocks
    private PaymentProcessingService paymentProcessingService;

    @Test
    void debitsSourceCreditsDestinationAndRecordsProcessedPayment() {
        UUID paymentId = UUID.randomUUID();
        PaymentAcceptedEvent event = event(paymentId, "account-001", "vendor-001");
        Account source = new Account("account-001", new BigDecimal("100.00"), "USD");
        Account destination = new Account("vendor-001", new BigDecimal("10.00"), "USD");

        when(processedPaymentRepository.existsById(paymentId)).thenReturn(false);
        when(accountRepository.findById("account-001")).thenReturn(java.util.Optional.of(source));
        when(accountRepository.findById("vendor-001")).thenReturn(java.util.Optional.of(destination));

        paymentProcessingService.process(event);

        assertThat(source.getBalance()).isEqualByComparingTo("75.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("35.00");
        verify(ledgerEntryRepository, org.mockito.Mockito.times(2))
                .save(org.mockito.ArgumentMatchers.any(LedgerEntry.class));
        verify(processedPaymentRepository).save(org.mockito.ArgumentMatchers.any(ProcessedPayment.class));
    }

    @Test
    void ignoresDuplicatePaymentEvent() {
        UUID paymentId = UUID.randomUUID();
        when(processedPaymentRepository.existsById(paymentId)).thenReturn(true);

        paymentProcessingService.process(event(paymentId, "account-001", "vendor-001"));

        verifyNoInteractions(accountRepository, ledgerEntryRepository);
        verify(processedPaymentRepository, never())
                .save(org.mockito.ArgumentMatchers.any(ProcessedPayment.class));
    }

    private PaymentAcceptedEvent event(UUID paymentId, String source, String destination) {
        return new PaymentAcceptedEvent(paymentId, "client-1", source, destination,
                new BigDecimal("25.00"), "USD", Instant.now());
    }
}
