package com.ledgerline.service;

import com.ledgerline.dto.AdminUserSummaryDto;
import com.ledgerline.dto.SystemStatsResponse;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.UpdateAccountStatusRequest;
import com.ledgerline.dto.UpdateUserStatusRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.AuditLog;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.UserRole;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing management capabilities for system administrators.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Lists all registered users and their associated accounts.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> {
            List<Account> accounts = accountRepository.findByUserId(user.getId());
            List<AdminUserSummaryDto.AccountDetailDto> accountDtos = accounts.stream()
                    .map(acc -> new AdminUserSummaryDto.AccountDetailDto(
                            acc.getId(),
                            acc.getAccountNumber(),
                            acc.getAccountType(),
                            acc.getBalance(),
                            acc.getCurrency(),
                            acc.getStatus(),
                            acc.getVersion(),
                            acc.getCreatedAt()
                    ))
                    .collect(Collectors.toList());

            return new AdminUserSummaryDto(
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
        });
    }

    /**
     * Freezes, unfreezes, or closes an account.
     */
    @Transactional
    public AdminUserSummaryDto.AccountDetailDto updateAccountStatus(String accountNumber,
                                                                   UpdateAccountStatusRequest request,
                                                                   String adminUsername) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(request.getStatus());
        account = accountRepository.save(account);

        User admin = userRepository.findByUsername(adminUsername).orElseThrow();

        // Write immutable audit trail
        auditLogRepository.save(new AuditLog(
                admin.getId(),
                admin.getUsername(),
                admin.getRole().name(),
                AuditAction.ACCOUNT_STATUS_UPDATED,
                "ACCOUNT",
                account.getAccountNumber(),
                "Status changed from " + oldStatus + " to " + request.getStatus() + ". Reason: " + request.getReason(),
                "127.0.0.1"
        ));

        return new AdminUserSummaryDto.AccountDetailDto(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getVersion(),
                account.getCreatedAt()
        );
    }

    /**
     * Suspends or activates a user.
     */
    @Transactional
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(request.getStatus());
        userRepository.save(user);

        User admin = userRepository.findByUsername(adminUsername).orElseThrow();

        auditLogRepository.save(new AuditLog(
                admin.getId(),
                admin.getUsername(),
                admin.getRole().name(),
                AuditAction.USER_STATUS_UPDATED,
                "USER",
                String.valueOf(user.getId()),
                "User status updated to " + request.getStatus() + ". Reason: " + request.getReason(),
                "127.0.0.1"
        ));
    }

    /**
     * Aggregates system metrics and live financial stats.
     */
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStatistics() {
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.findByRole(UserRole.ROLE_CUSTOMER).size();
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long frozenAccounts = accountRepository.countByStatus(AccountStatus.FROZEN);

        BigDecimal totalDeposits = transactionRepository.calculateTotalDeposits();
        BigDecimal totalWithdrawals = transactionRepository.calculateTotalWithdrawals();
        BigDecimal totalTransfers = transactionRepository.calculateTotalTransfers();
        BigDecimal totalActiveBalance = accountRepository.calculateTotalActiveBalance();
        long totalTransactions = transactionRepository.count();

        return new SystemStatsResponse(
                totalUsers,
                totalCustomers,
                totalAccounts,
                activeAccounts,
                frozenAccounts,
                totalDeposits,
                totalWithdrawals,
                totalTransfers,
                totalActiveBalance,
                totalTransactions
        );
    }

    /**
     * Retrieves all system transactions with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(TransactionStatus status, Pageable pageable) {
        if (status != null) {
            return transactionRepository.findByStatus(status, pageable)
                    .map(this::mapToTransactionResponse);
        }
        return transactionRepository.findAll(pageable)
                .map(this::mapToTransactionResponse);
    }

    private TransactionResponse mapToTransactionResponse(com.ledgerline.entity.Transaction txn) {
        return new TransactionResponse(
                txn.getTransactionReference(),
                txn.getIdempotencyKey(),
                txn.getSourceAccount() != null ? txn.getSourceAccount().getAccountNumber() : null,
                txn.getTargetAccount() != null ? txn.getTargetAccount().getAccountNumber() : null,
                txn.getAmount(),
                txn.getType(),
                txn.getStatus(),
                txn.getDescription(),
                null,
                txn.getCreatedAt(),
                txn.getCompletedAt()
        );
    }
}
