package com.payflow.processor.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void reportsBalancedTransferWhenDebitAndCreditMatch() {
        UUID paymentId = UUID.randomUUID();
        when(ledgerEntryRepository.findByPaymentId(paymentId)).thenReturn(List.of(
                new LedgerEntry(paymentId, "account-001", LedgerEntry.EntryType.DEBIT,
                        new BigDecimal("25.00"), "USD"),
                new LedgerEntry(paymentId, "vendor-001", LedgerEntry.EntryType.CREDIT,
                        new BigDecimal("25.00"), "USD")));

        ReconciliationResult result = reconciliationService.reconcile(paymentId);

        assertThat(result.balanced()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("25.00");
        assertThat(result.reason()).isEqualTo("DEBIT_CREDIT_MATCH");
    }

    @Test
    void reportsMismatchWhenLedgerAmountsDiffer() {
        UUID paymentId = UUID.randomUUID();
        when(ledgerEntryRepository.findByPaymentId(paymentId)).thenReturn(List.of(
                new LedgerEntry(paymentId, "account-001", LedgerEntry.EntryType.DEBIT,
                        new BigDecimal("25.00"), "USD"),
                new LedgerEntry(paymentId, "vendor-001", LedgerEntry.EntryType.CREDIT,
                        new BigDecimal("20.00"), "USD")));

        ReconciliationResult result = reconciliationService.reconcile(paymentId);

        assertThat(result.balanced()).isFalse();
        assertThat(result.reason()).isEqualTo("AMOUNT_MISMATCH");
    }
}