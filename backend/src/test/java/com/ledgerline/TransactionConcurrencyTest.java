package com.ledgerline;

import com.ledgerline.dto.TransferRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concurrency integration test verifying zero balance drift and thread-safety
 * under simultaneous transfer requests on the same bank account.
 */
@SpringBootTest
public class TransactionConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private Account senderAccount;
    private Account receiverAccount;
    private User testUser;

    @BeforeEach
    public void setup() {
        // Clean or prepare test accounts
        testUser = userRepository.findByUsername("concurrency_user").orElseGet(() -> {
            User u = new User("concurrency_user", "conc@example.com", "hash", "Concurrency User", "+123", UserRole.ROLE_CUSTOMER);
            return userRepository.save(u);
        });

        senderAccount = accountRepository.findByAccountNumber("LL-CONC-SENDER").orElseGet(() -> {
            Account acc = new Account("LL-CONC-SENDER", testUser, AccountType.SAVINGS, new BigDecimal("1000.00"), "USD");
            return accountRepository.save(acc);
        });
        senderAccount.setBalance(new BigDecimal("1000.00"));
        accountRepository.save(senderAccount);

        receiverAccount = accountRepository.findByAccountNumber("LL-CONC-RECEIVER").orElseGet(() -> {
            Account acc = new Account("LL-CONC-RECEIVER", testUser, AccountType.SAVINGS, new BigDecimal("0.00"), "USD");
            return accountRepository.save(acc);
        });
        receiverAccount.setBalance(new BigDecimal("0.00"));
        accountRepository.save(receiverAccount);
    }

    @Test
    @DisplayName("Verify thread-safe balance updates with zero balance drift across 10 concurrent threads")
    public void testConcurrentTransfersNoBalanceDrift() throws InterruptedException {
        int numberOfThreads = 10;
        BigDecimal transferAmount = new BigDecimal("50.00"); // 10 * $50 = $500 total transfer
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        String testBatchId = String.valueOf(System.currentTimeMillis());
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to hit simultaneously
                    TransferRequest req = new TransferRequest(
                            senderAccount.getAccountNumber(),
                            receiverAccount.getAccountNumber(),
                            transferAmount,
                            "Concurrent Thread " + index,
                            "CONC-KEY-" + testBatchId + "-" + index
                    );
                    transactionService.transferFunds(req, testUser.getUsername());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Reload balances from database
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();

        // 10 successful transfers of $50 = $500 transferred
        assertEquals(10, successCount.get(), "All 10 concurrent transfers must succeed");
        assertEquals(new BigDecimal("500.00"), updatedSender.getBalance(), "Sender balance must be exactly $500.00");
        assertEquals(new BigDecimal("500.00"), updatedReceiver.getBalance(), "Receiver balance must be exactly $500.00");

        // Mathematical conservation check: Total money in system must remain constant ($1000.00)
        BigDecimal totalMoney = updatedSender.getBalance().add(updatedReceiver.getBalance());
        assertEquals(new BigDecimal("1000.00"), totalMoney, "Total system money must strictly equal initial money ($1000.00)");
    }
}
