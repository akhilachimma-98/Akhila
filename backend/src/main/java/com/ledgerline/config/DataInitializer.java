package com.ledgerline.config;

import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Seeds default accounts for demo and testing if database is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Database is empty. Seeding initial users and accounts for LedgerLine...");

            // 1. Seed Admin
            RegisterRequest adminReq = new RegisterRequest();
            adminReq.setUsername("admin");
            adminReq.setEmail("admin@ledgerline.com");
            adminReq.setPassword("Admin@123");
            adminReq.setFullName("System Administrator");
            adminReq.setPhoneNumber("+1234567890");
            adminReq.setRole(UserRole.ROLE_ADMIN);
            authService.register(adminReq);
            log.info("Admin created: admin / Admin@123 (ROLE_ADMIN)");

            // 2. Seed Auditor
            RegisterRequest auditorReq = new RegisterRequest();
            auditorReq.setUsername("auditor");
            auditorReq.setEmail("auditor@ledgerline.com");
            auditorReq.setPassword("Auditor@123");
            auditorReq.setFullName("Financial Auditor");
            auditorReq.setPhoneNumber("+1234567891");
            auditorReq.setRole(UserRole.ROLE_AUDITOR);
            authService.register(auditorReq);
            log.info("Auditor created: auditor / Auditor@123 (ROLE_AUDITOR)");

            // 3. Seed Demo Customer 1 (John Doe)
            RegisterRequest customer1 = new RegisterRequest();
            customer1.setUsername("john_doe");
            customer1.setEmail("john@example.com");
            customer1.setPassword("Customer@123");
            customer1.setFullName("John Doe");
            customer1.setPhoneNumber("+1234567892");
            customer1.setRole(UserRole.ROLE_CUSTOMER);
            customer1.setAccountType(AccountType.SAVINGS);
            customer1.setInitialDeposit(new BigDecimal("1000.00"));
            authService.register(customer1);
            log.info("Customer 1 created: john_doe / Customer@123 (Balance: $1000.00)");

            // 4. Seed Demo Customer 2 (Jane Smith)
            RegisterRequest customer2 = new RegisterRequest();
            customer2.setUsername("jane_smith");
            customer2.setEmail("jane@example.com");
            customer2.setPassword("Customer@123");
            customer2.setFullName("Jane Smith");
            customer2.setPhoneNumber("+1234567893");
            customer2.setRole(UserRole.ROLE_CUSTOMER);
            customer2.setAccountType(AccountType.CHECKING);
            customer2.setInitialDeposit(new BigDecimal("500.00"));
            authService.register(customer2);
            log.info("Customer 2 created: jane_smith / Customer@123 (Balance: $500.00)");

            log.info("LedgerLine seed initialization completed successfully!");
        }
    }
}
