package com.payflow.processor.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessingService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ProcessedPaymentRepository processedPaymentRepository;

    public PaymentProcessingService(AccountRepository accountRepository,
                                    LedgerEntryRepository ledgerEntryRepository,
                                    ProcessedPaymentRepository processedPaymentRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.processedPaymentRepository = processedPaymentRepository;
    }

    @Transactional
    public void process(PaymentAcceptedEvent event) {
        if (processedPaymentRepository.existsById(event.paymentId())) {
            return;
        }

        Account source = accountRepository.findById(event.sourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: "
                        + event.sourceAccountId()));
        Account destination = accountRepository.findById(event.destinationAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: "
                        + event.destinationAccountId()));

        validateCurrency(source, destination, event);
        source.debit(event.amount());
        destination.credit(event.amount());

        accountRepository.save(source);
        accountRepository.save(destination);
        ledgerEntryRepository.save(new LedgerEntry(event.paymentId(), source.getAccountId(),
                LedgerEntry.EntryType.DEBIT, event.amount(), event.currency()));
        ledgerEntryRepository.save(new LedgerEntry(event.paymentId(), destination.getAccountId(),
                LedgerEntry.EntryType.CREDIT, event.amount(), event.currency()));
        processedPaymentRepository.save(new ProcessedPayment(event.paymentId()));
    }

    private void validateCurrency(Account source, Account destination, PaymentAcceptedEvent event) {
        if (!source.getCurrency().equals(event.currency())
                || !destination.getCurrency().equals(event.currency())) {
            throw new IllegalArgumentException("Payment currency does not match both accounts");
        }
    }
}
