package com.ledgerline.service;

import com.ledgerline.dto.AuthResponse;
import com.ledgerline.dto.LoginRequest;
import com.ledgerline.dto.RegisterRequest;
import com.ledgerline.dto.UserProfileResponse;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.AuditLog;
import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.entity.Transaction;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.LedgerEntryType;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.enums.UserStatus;
import com.ledgerline.exception.BadRequestException;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.security.CustomUserDetails;
import com.ledgerline.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing user registration, authentication, password verification,
 * and customer bank account initialization.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       TransactionRepository transactionRepository,
                       LedgerEntryRepository ledgerEntryRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user with BCrypt password hashing.
     * Automatically provisions a default account with immutable ledger lines if Customer.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        // 1. Create and persist user
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getRole()
        );
        user = userRepository.save(user);

        String accountNumber = null;
        BigDecimal accountBalance = null;

        // 2. Provision account if user is a Customer
        if (user.getRole() == UserRole.ROLE_CUSTOMER) {
            accountNumber = generateUniqueAccountNumber();
            BigDecimal initialDeposit = request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0
                    ? request.getInitialDeposit().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            Account account = new Account(accountNumber, user, request.getAccountType(), initialDeposit, "USD");
            account = accountRepository.save(account);
            accountBalance = account.getBalance();

            // If initial deposit is provided, write double-entry transaction and ledger record
            if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                String txnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
                Transaction depositTxn = new Transaction(
                        txnRef,
                        null,
                        null,
                        account,
                        initialDeposit,
                        TransactionType.DEPOSIT,
                        "Initial Account Opening Deposit"
                );
                depositTxn.setStatus(TransactionStatus.COMPLETED);
                depositTxn.setCompletedAt(LocalDateTime.now());
                depositTxn = transactionRepository.save(depositTxn);

                LedgerEntry creditEntry = new LedgerEntry(
                        depositTxn,
                        account,
                        LedgerEntryType.CREDIT,
                        initialDeposit,
                        account.getBalance(),
                        "Initial Account Opening Credit"
                );
                ledgerEntryRepository.save(creditEntry);
            }
        }

        // 3. Write immutable audit log
        auditLogRepository.save(new AuditLog(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                AuditAction.USER_REGISTERED,
                "USER",
                String.valueOf(user.getId()),
                "User registered with role: " + user.getRole(),
                "127.0.0.1"
        ));

        // 4. Issue JWT token
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                accountNumber,
                accountBalance
        );
    }

    /**
     * Authenticates user credentials and returns JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is " + user.getStatus());
        }

        String token = jwtService.generateToken(userDetails);

        String accountNumber = null;
        BigDecimal accountBalance = null;

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        if (!accounts.isEmpty()) {
            Account primaryAccount = accounts.get(0);
            accountNumber = primaryAccount.getAccountNumber();
            accountBalance = primaryAccount.getBalance();
        }

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                accountNumber,
                accountBalance
        );
    }

    /**
     * Retrieves current logged in user's profile and accounts.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + username));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        List<UserProfileResponse.AccountSummaryDto> accountDtos = accounts.stream()
                .map(acc -> new UserProfileResponse.AccountSummaryDto(
                        acc.getId(),
                        acc.getAccountNumber(),
                        acc.getAccountType(),
                        acc.getBalance(),
                        acc.getCurrency(),
                        acc.getStatus()
                ))
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                accountDtos
        );
    }

    /**
     * Generates an unambiguous, unique 12-digit bank account number prefixed with 'LL'.
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            long number = 100_000_000_000L + (Math.abs(secureRandom.nextLong()) % 900_000_000_000L);
            accountNumber = "LL" + number;
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
