package com.ledgerline.service;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.LedgerEntryResponse;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.AuditLog;
import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.entity.Transaction;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.LedgerEntryType;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.exception.BadRequestException;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Financial Transaction Engine for LedgerLine.
 * Enforces ACID transactions, deadlock-free pessimistic locking,
 * idempotency validation, and append-only double-entry ledger bookkeeping.
 */
@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              AuditLogRepository auditLogRepository,
                              UserRepository userRepository,
                              IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Executes atomic account-to-account money transfer with strict safeguards.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transferFunds(TransferRequest request, String currentUsername) {
        String idempotencyKey = request.getIdempotencyKey();

        // 1. Idempotency Check: return cached response if request was already processed
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<TransactionResponse> existing = idempotencyService.findExistingResponse(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        // Register in-progress idempotency tracking
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.startTracking(idempotencyKey, currentUser, "/api/transactions/transfer", request);
        }

        try {
            String sourceAccNum = request.getSourceAccountNumber().trim();
            String targetAccNum = request.getTargetAccountNumber().trim();

            if (sourceAccNum.equalsIgnoreCase(targetAccNum)) {
                throw new BadRequestException("Source and target account cannot be the same");
            }

            BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Transfer amount must be strictly greater than zero");
            }

            // 3. DEADLOCK PREVENTION LOCKING ORDER BY ACCOUNT NUMBER
            // Always lock the alphabetically smaller account number first, then larger account number
            Account firstLocked;
            Account secondLocked;
            if (sourceAccNum.compareToIgnoreCase(targetAccNum) < 0) {
                firstLocked = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                        .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccNum));
                secondLocked = accountRepository.findByAccountNumberWithLock(targetAccNum)
                        .orElseThrow(() -> new ResourceNotFoundException("Target account not found: " + targetAccNum));
            } else {
                firstLocked = accountRepository.findByAccountNumberWithLock(targetAccNum)
                        .orElseThrow(() -> new ResourceNotFoundException("Target account not found: " + targetAccNum));
                secondLocked = accountRepository.findByAccountNumberWithLock(sourceAccNum)
                        .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccNum));
            }

            Account sourceAccount = sourceAccNum.equalsIgnoreCase(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;
            Account targetAccount = targetAccNum.equalsIgnoreCase(firstLocked.getAccountNumber()) ? firstLocked : secondLocked;

            // Authorization Check: Customer can only transfer from their own account
            if (currentUser.getRole() == UserRole.ROLE_CUSTOMER && !sourceAccount.getUser().getId().equals(currentUser.getId())) {
                throw new BadRequestException("Unauthorized: You do not have ownership of source account " + sourceAccNum);
            }

            // 4. Operational Status Verification
            if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new BadRequestException("Source account is " + sourceAccount.getStatus() + ". Transfers are disallowed.");
            }
            if (targetAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new BadRequestException("Target account is " + targetAccount.getStatus() + ". Transfers are disallowed.");
            }

            // 5. Balance Adequacy Verification
            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                throw new BadRequestException("Insufficient funds. Available balance: " + sourceAccount.getBalance() + " " + sourceAccount.getCurrency());
            }

            // 6. Execute Balance Mutations
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
            targetAccount.setBalance(targetAccount.getBalance().add(amount));

            accountRepository.save(sourceAccount);
            accountRepository.save(targetAccount);

            // 7. Record Business Transaction
            String txnRef = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            Transaction transaction = new Transaction(
                    txnRef,
                    idempotencyKey,
                    sourceAccount,
                    targetAccount,
                    amount,
                    TransactionType.TRANSFER,
                    request.getDescription()
            );
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            // 8. Record Double-Entry Append-Only Ledger Lines
            // Debit for Source (Sender)
            LedgerEntry debitEntry = new LedgerEntry(
                    transaction,
                    sourceAccount,
                    LedgerEntryType.DEBIT,
                    amount,
                    sourceAccount.getBalance(),
                    "Transfer to " + targetAccount.getAccountNumber() + ": " + request.getDescription()
            );
            ledgerEntryRepository.save(debitEntry);

            // Credit for Target (Receiver)
            LedgerEntry creditEntry = new LedgerEntry(
                    transaction,
                    targetAccount,
                    LedgerEntryType.CREDIT,
                    amount,
                    targetAccount.getBalance(),
                    "Transfer from " + sourceAccount.getAccountNumber() + ": " + request.getDescription()
            );
            ledgerEntryRepository.save(creditEntry);

            // 9. Write Security Audit Log
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getRole().name(),
                    AuditAction.TRANSFER_COMPLETED,
                    "TRANSACTION",
                    txnRef,
                    "Transferred " + amount + " " + sourceAccount.getCurrency() + " from " + sourceAccount.getAccountNumber() + " to " + targetAccount.getAccountNumber(),
                    "127.0.0.1"
            ));

            TransactionResponse response = new TransactionResponse(
                    transaction.getTransactionReference(),
                    transaction.getIdempotencyKey(),
                    sourceAccount.getAccountNumber(),
                    targetAccount.getAccountNumber(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDescription(),
                    sourceAccount.getBalance(),
                    transaction.getCreatedAt(),
                    transaction.getCompletedAt()
            );

            // Mark Idempotency as completed with cached response
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markCompleted(idempotencyKey, response);
            }

            return response;

        } catch (Exception ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markFailed(idempotencyKey, ex.getMessage());
            }
            throw ex;
        }
    }

    /**
     * Executes atomic fund deposit.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse depositFunds(DepositRequest request, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Deposit amount must be strictly greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Cannot deposit into " + account.getStatus() + " account");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        String txnRef = "DEP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Transaction transaction = new Transaction(
                txnRef,
                null,
                null,
                account,
                amount,
                TransactionType.DEPOSIT,
                request.getDescription()
        );
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                account,
                LedgerEntryType.CREDIT,
                amount,
                account.getBalance(),
                "Deposit: " + request.getDescription()
        );
        ledgerEntryRepository.save(creditEntry);

        auditLogRepository.save(new AuditLog(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getRole().name(),
                AuditAction.DEPOSIT_COMPLETED,
                "ACCOUNT",
                account.getAccountNumber(),
                "Deposited " + amount + " " + account.getCurrency(),
                "127.0.0.1"
        ));

        return new TransactionResponse(
                transaction.getTransactionReference(),
                null,
                null,
                account.getAccountNumber(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                account.getBalance(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }

    /**
     * Executes atomic fund withdrawal.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdrawFunds(WithdrawRequest request, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Withdrawal amount must be strictly greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        // Ownership validation
        if (currentUser.getRole() == UserRole.ROLE_CUSTOMER && !account.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Unauthorized: You do not own account " + request.getAccountNumber());
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Cannot withdraw from " + account.getStatus() + " account");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance for withdrawal. Current: " + account.getBalance());
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        String txnRef = "WTH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Transaction transaction = new Transaction(
                txnRef,
                null,
                account,
                null,
                amount,
                TransactionType.WITHDRAWAL,
                request.getDescription()
        );
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        LedgerEntry debitEntry = new LedgerEntry(
                transaction,
                account,
                LedgerEntryType.DEBIT,
                amount,
                account.getBalance(),
                "Withdrawal: " + request.getDescription()
        );
        ledgerEntryRepository.save(debitEntry);

        auditLogRepository.save(new AuditLog(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getRole().name(),
                AuditAction.WITHDRAWAL_COMPLETED,
                "ACCOUNT",
                account.getAccountNumber(),
                "Withdrew " + amount + " " + account.getCurrency(),
                "127.0.0.1"
        ));

        return new TransactionResponse(
                transaction.getTransactionReference(),
                null,
                account.getAccountNumber(),
                null,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                account.getBalance(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }

    /**
     * Retrieves paginated transaction history for an account.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(String accountNumber, String currentUsername, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
        if (currentUser.getRole() == UserRole.ROLE_CUSTOMER && !account.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Unauthorized access to account transaction history");
        }

        return transactionRepository.findByAccountId(account.getId(), pageable)
                .map(txn -> new TransactionResponse(
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
                ));
    }

    /**
     * Retrieves passbook / ledger statement with running balance snapshots.
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getAccountLedgerStatement(String accountNumber, String currentUsername, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();
        if (currentUser.getRole() == UserRole.ROLE_CUSTOMER && !account.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Unauthorized access to account ledger statement");
        }

        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(entry -> new LedgerEntryResponse(
                        entry.getId(),
                        entry.getTransaction().getTransactionReference(),
                        entry.getAccount().getAccountNumber(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getBalanceAfter(),
                        entry.getDescription(),
                        entry.getCreatedAt()
                ));
    }
}
