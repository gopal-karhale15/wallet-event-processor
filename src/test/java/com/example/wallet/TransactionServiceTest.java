package com.example.wallet;

import com.example.wallet.dto.TransactionRequest;
import com.example.wallet.dto.TransactionResponse;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.ConflictException;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.repository.TransactionRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.service.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {

        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        userId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.save(wallet);
    }

    @Test
    @DisplayName(
            "Happy Path Test - Processes a single valid debit transaction successfully."
    )
    void happyPathTest() {

        System.out.println(
                "\n[TEST] Happy Path: " +
                        "Processes a single valid debit transaction successfully."
        );

        TransactionRequest request = new TransactionRequest();

        request.setTransactionId(UUID.randomUUID());
        request.setUserId(userId);
        request.setAmount(new BigDecimal("100.00"));
        request.setType("DEBIT");

        TransactionResponse response =
                transactionService.processTransaction(request);

        assertEquals(
                new BigDecimal("400.00"),
                response.getBalance()
        );

        System.out.println(
                "[RESULT] Transaction successful. Balance = ₹"
                        + response.getBalance()
        );
    }

    @Test
    @DisplayName(
            "Idempotency Test - Sends 3 identical transaction IDs simultaneously. Ensures the balance is only deducted once."
    )
    void idempotencyTest() throws Exception {

        System.out.println(
                "\n[TEST] Idempotency: " +
                        "Sends 3 identical transaction IDs simultaneously."
        );

        UUID transactionId = UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            tasks.add(() -> {

                TransactionRequest request =
                        new TransactionRequest();

                request.setTransactionId(transactionId);
                request.setUserId(userId);
                request.setAmount(new BigDecimal("100.00"));
                request.setType("DEBIT");

                try {

                    transactionService
                            .processTransaction(request);

                    return "SUCCESS";

                } catch (ConflictException ex) {

                    return "CONFLICT";
                }
            });
        }

        List<Future<String>> results =
                executor.invokeAll(tasks);

        executor.shutdown();

        long successCount = results.stream()
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter("SUCCESS"::equals)
                .count();

        Wallet wallet =
                walletRepository.findById(userId).orElseThrow();

        assertEquals(1, successCount);

        assertEquals(
                new BigDecimal("400.00"),
                wallet.getBalance()
        );

        System.out.println(
                "[RESULT] Successful requests = "
                        + successCount
        );

        System.out.println(
                "[RESULT] Final balance = ₹"
                        + wallet.getBalance()
        );
    }

    @Test
    @DisplayName(
            "Race Condition Test - Sends 10 concurrent ₹100 debit requests to a ₹500 wallet."
    )
    void raceConditionTest() throws Exception {

        System.out.println(
                "\n[TEST] Race Condition: " +
                        "10 concurrent ₹100 debit requests on ₹500 balance."
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            tasks.add(() -> {

                TransactionRequest request =
                        new TransactionRequest();

                request.setTransactionId(
                        UUID.randomUUID()
                );

                request.setUserId(userId);

                request.setAmount(
                        new BigDecimal("100.00")
                );

                request.setType("DEBIT");

                try {

                    transactionService
                            .processTransaction(request);

                    return "SUCCESS";

                } catch (InsufficientFundsException ex) {

                    return "INSUFFICIENT_FUNDS";
                }
            });
        }

        List<Future<String>> results =
                executor.invokeAll(tasks);

        executor.shutdown();

        long successCount = 0;
        long insufficientCount = 0;

        for (Future<String> result : results) {

            String status = result.get();

            if ("SUCCESS".equals(status)) {
                successCount++;
            }

            if ("INSUFFICIENT_FUNDS".equals(status)) {
                insufficientCount++;
            }
        }

        Wallet wallet =
                walletRepository.findById(userId).orElseThrow();

        assertEquals(5, successCount);

        assertEquals(5, insufficientCount);

        assertEquals(
                new BigDecimal("0.00"),
                wallet.getBalance()
        );

        System.out.println(
                "[RESULT] Successful requests = "
                        + successCount
        );

        System.out.println(
                "[RESULT] Insufficient funds requests = "
                        + insufficientCount
        );

        System.out.println(
                "[RESULT] Final balance = ₹"
                        + wallet.getBalance()
        );
    }
}