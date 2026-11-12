package com.ledgerline.service;

import com.ledgerline.dto.AuditLogResponse;
import com.ledgerline.dto.LedgerEntryResponse;
import com.ledgerline.dto.ReconciliationReportResponse;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.AuditLogRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only audit and financial reconciliation service.
 * Auditors use this to verify mathematical integrity and inspect immutable audit trails.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Retrieves system audit logs with optional filtering.
     */
    public Page<AuditLogResponse> getAuditLogs(AuditAction action, Pageable pageable) {
        if (action != null) {
            return auditLogRepository.findByAction(action, pageable).map(this::mapToAuditLogResponse);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapToAuditLogResponse);
    }

    /**
     * Retrieves all system-wide immutable double-entry ledger lines.
     */
    public Page<LedgerEntryResponse> getAllLedgerEntries(Pageable pageable) {
        return ledgerEntryRepository.findAll(pageable).map(entry -> new LedgerEntryResponse(
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

    /**
     * Executes automated bank balance reconciliation check:
     * (Total Deposits - Total Withdrawals) must match the sum of all customer account balances.
     */
    public ReconciliationReportResponse performReconciliation() {
        BigDecimal totalDeposits = transactionRepository.calculateTotalDeposits();
        BigDecimal totalWithdrawals = transactionRepository.calculateTotalWithdrawals();
        BigDecimal expectedReserve = totalDeposits.subtract(totalWithdrawals);
        BigDecimal actualAccountsBalanceSum = accountRepository.calculateTotalActiveBalance();

        BigDecimal discrepancy = expectedReserve.subtract(actualAccountsBalanceSum).abs();
        String reconciliationStatus = (discrepancy.compareTo(BigDecimal.ZERO) == 0)
                ? "RECONCILED"
                : "DISCREPANCY_DETECTED";

        long totalLedgerEntriesCount = ledgerEntryRepository.count();

        return new ReconciliationReportResponse(
                totalDeposits,
                totalWithdrawals,
                expectedReserve,
                actualAccountsBalanceSum,
                discrepancy,
                reconciliationStatus,
                totalLedgerEntriesCount,
                LocalDateTime.now()
        );
    }

    private AuditLogResponse mapToAuditLogResponse(com.ledgerline.entity.AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorUsername(),
                log.getActorRole(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
