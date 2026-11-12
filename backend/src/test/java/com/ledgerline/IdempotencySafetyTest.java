package com.ledgerline;

import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.exception.BadRequestException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class IdempotencySafetyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Account sender;
    private Account receiver;
    private User customer;

    @BeforeEach
    public void setup() {
        customer = userRepository.findByUsername("safety_user").orElseGet(() -> {
            User u = new User("safety_user", "safety@example.com", "hash", "Safety User", "+123", UserRole.ROLE_CUSTOMER);
            return userRepository.save(u);
        });

        sender = accountRepository.findByAccountNumber("LL-SAFE-SENDER").orElseGet(() -> {
            Account acc = new Account("LL-SAFE-SENDER", customer, AccountType.SAVINGS, new BigDecimal("200.00"), "USD");
            return accountRepository.save(acc);
        });
        sender.setBalance(new BigDecimal("200.00"));
        sender.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(sender);

        receiver = accountRepository.findByAccountNumber("LL-SAFE-RECEIVER").orElseGet(() -> {
            Account acc = new Account("LL-SAFE-RECEIVER", customer, AccountType.SAVINGS, new BigDecimal("50.00"), "USD");
            return accountRepository.save(acc);
        });
        receiver.setBalance(new BigDecimal("50.00"));
        receiver.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(receiver);
    }

    @Test
    @DisplayName("Idempotency Key reuse must return cached response without double money deduction")
    public void testIdempotencyPreventsDoubleDeduction() {
        String key = "IDEM-TEST-REUSE-" + System.currentTimeMillis();
        TransferRequest req = new TransferRequest(
                sender.getAccountNumber(),
                receiver.getAccountNumber(),
                new BigDecimal("50.00"),
                "First execution",
                key
        );

        // First attempt: should deduct $50
        TransactionResponse firstResponse = transactionService.transferFunds(req, customer.getUsername());
        Account senderAfterFirst = accountRepository.findById(sender.getId()).orElseThrow();
        assertEquals(new BigDecimal("150.00"), senderAfterFirst.getBalance());

        // Second attempt with SAME key (simulating network retry / double click):
        TransactionResponse secondResponse = transactionService.transferFunds(req, customer.getUsername());
        Account senderAfterSecond = accountRepository.findById(sender.getId()).orElseThrow();

        // Must still be $150.00, NOT $100.00!
        assertEquals(new BigDecimal("150.00"), senderAfterSecond.getBalance(), "Balance must not be deducted twice on idempotency key replay");
        assertEquals(firstResponse.getTransactionReference(), secondResponse.getTransactionReference(), "Must return identical cached transaction reference");
    }

    @Test
    @DisplayName("Transfers from FROZEN accounts must be strictly blocked")
    public void testFrozenAccountBlocked() {
        sender.setStatus(AccountStatus.FROZEN);
        accountRepository.save(sender);

        TransferRequest req = new TransferRequest(
                sender.getAccountNumber(),
                receiver.getAccountNumber(),
                new BigDecimal("10.00"),
                "Frozen test",
                "KEY-FROZEN-" + System.currentTimeMillis()
        );

        assertThrows(BadRequestException.class, () -> {
            transactionService.transferFunds(req, customer.getUsername());
        });
    }

    @Test
    @DisplayName("Transfers exceeding account balance must throw Insufficient Funds")
    public void testInsufficientFundsBlocked() {
        TransferRequest req = new TransferRequest(
                sender.getAccountNumber(),
                receiver.getAccountNumber(),
                new BigDecimal("9999.00"), // Exceeds $200
                "Overdraft test",
                "KEY-OVERDRAFT-" + System.currentTimeMillis()
        );

        assertThrows(BadRequestException.class, () -> {
            transactionService.transferFunds(req, customer.getUsername());
        });
    }
}
