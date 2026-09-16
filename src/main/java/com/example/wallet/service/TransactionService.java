package com.example.wallet.service;

import com.example.wallet.dto.TransactionRequest;
import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.entity.Transaction;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.ConflictException;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.repository.TransactionRepository;
import com.example.wallet.repository.WalletRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(
            TransactionRequest request
    ) {

        // Database-level pessimistic lock
        Wallet wallet = walletRepository
                .findById(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found"
                        )
                );

        // Idempotency check
        if (transactionRepository
                .findByTransactionId(request.getTransactionId())
                .isPresent()) {

            throw new ConflictException(
                    "Transaction already processed: "
                            + request.getTransactionId()
            );
        }

        // Currently assignment requires DEBIT
        if (!"DEBIT".equalsIgnoreCase(request.getType())) {
            throw new IllegalArgumentException(
                    "Only DEBIT transactions are supported"
            );
        }

        BigDecimal currentBalance = wallet.getBalance();

        // Prevent negative balance
        if (currentBalance.compareTo(request.getAmount()) < 0) {

            throw new InsufficientFundsException(
                    "Insufficient funds"
            );
        }

        BigDecimal newBalance =
                currentBalance.subtract(request.getAmount());

        wallet.setBalance(newBalance);

        walletRepository.save(wallet);

        Transaction transaction = new Transaction();

        transaction.setTransactionId(
                request.getTransactionId()
        );

        transaction.setUserId(
                request.getUserId()
        );

        transaction.setAmount(
                request.getAmount()
        );

        transaction.setType(
                request.getType().toUpperCase()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        try {

            transactionRepository.saveAndFlush(transaction);

        } catch (DataIntegrityViolationException ex) {

            /*
             * Unique transaction_id protects against
             * duplicate webhook requests.
             */
            throw new ConflictException(
                    "Transaction already processed"
            );
        }

        return new TransactionResponse(
                "Transaction processed successfully",
                request.getTransactionId(),
                newBalance
        );
    }
}