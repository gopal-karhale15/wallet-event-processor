package com.example.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionResponse {

    private String message;
    private UUID transactionId;
    private BigDecimal balance;

    public TransactionResponse(
            String message,
            UUID transactionId,
            BigDecimal balance
    ) {
        this.message = message;
        this.transactionId = transactionId;
        this.balance = balance;
    }

    public String getMessage() {
        return message;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}