package com.payflow.processor.payment;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String accountId) {
        super("Insufficient funds in account: " + accountId);
    }
}
