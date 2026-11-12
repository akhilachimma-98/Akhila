package com.ledgerline;

import com.ledgerline.dto.AuthResponse;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.Transaction;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.TransactionType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RegistrationWithDepositTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Verify customer registration with initial deposit automatically generates transaction_id without errors")
    public void testCustomerRegistrationWithInitialDeposit() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String username = "cust_" + uniqueSuffix;
        String email = "cust_" + uniqueSuffix + "@example.com";
        BigDecimal initialDeposit = new BigDecimal("250.00");

        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("SecurePass@123");
        request.setFullName("Test Customer " + uniqueSuffix);
        request.setPhoneNumber("+919876543210");
        request.setRole(UserRole.ROLE_CUSTOMER);
        request.setAccountType(AccountType.SAVINGS);
        request.setInitialDeposit(initialDeposit);

        // 1. Execute registration
        AuthResponse response = authService.register(request);

        // 2. Validate AuthResponse
        assertNotNull(response, "Auth response should not be null");
        assertNotNull(response.getToken(), "JWT token should be generated");
        assertNotNull(response.getAccountNumber(), "Bank account number should be generated");
        assertEquals(0, initialDeposit.compareTo(response.getAccountBalance()), "Account balance must match initial deposit");

        // 3. Validate Account in DB
        Account account = accountRepository.findByAccountNumber(response.getAccountNumber()).orElseThrow();
        assertEquals(0, initialDeposit.compareTo(account.getBalance()), "Database balance must match initial deposit");

        // 4. Validate Transaction in DB
        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertEquals(1, transactions.size(), "There should be exactly one transaction for initial deposit");

        Transaction depositTxn = transactions.get(0);
        assertNotNull(depositTxn.getId(), "Database primary key id must be auto-generated");
        assertNotNull(depositTxn.getTransactionId(), "transactionId must be automatically populated");
        assertNotNull(depositTxn.getTransactionReference(), "transactionReference must be automatically populated");
        assertTrue(depositTxn.getTransactionId().startsWith("TXN-"), "transactionId must have TXN- prefix");
        assertEquals(TransactionType.DEPOSIT, depositTxn.getType(), "Transaction type must be DEPOSIT");
        assertEquals(0, initialDeposit.compareTo(depositTxn.getAmount()), "Transaction amount must be 250.00");
    }
}
