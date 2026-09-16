package com.payflow.processor.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public ReconciliationResult reconcile(UUID paymentId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByPaymentId(paymentId);
        if (entries.size() != 2) {
            return ReconciliationResult.failed(paymentId, "EXPECTED_TWO_LEDGER_ENTRIES");
        }

        LedgerEntry debit = findByType(entries, LedgerEntry.EntryType.DEBIT);
        LedgerEntry credit = findByType(entries, LedgerEntry.EntryType.CREDIT);
        if (debit == null || credit == null) {
            return ReconciliationResult.failed(paymentId, "MISSING_DEBIT_OR_CREDIT");
        }
        if (debit.getAmount().compareTo(credit.getAmount()) != 0) {
            return ReconciliationResult.failed(paymentId, "AMOUNT_MISMATCH");
        }
        if (!debit.getCurrency().equals(credit.getCurrency())) {
            return ReconciliationResult.failed(paymentId, "CURRENCY_MISMATCH");
        }

        return ReconciliationResult.balanced(paymentId, debit.getAmount(), debit.getCurrency());
    }

    private LedgerEntry findByType(List<LedgerEntry> entries, LedgerEntry.EntryType type) {
        return entries.stream()
                .filter(entry -> entry.getType() == type)
                .findFirst()
                .orElse(null);
    }
}