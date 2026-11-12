package com.ledgerline.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * High-value financial integrity report proving whether total money
 * in accounts strictly reconciles with the net transactional ledger.
 */
public class ReconciliationReportResponse {

    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal expectedSystemReserve;
    private BigDecimal actualAccountsBalanceSum;
    private BigDecimal discrepancy;
    private String reconciliationStatus; // "RECONCILED" or "DISCREPANCY_DETECTED"
    private long totalLedgerEntriesCount;
    private LocalDateTime generatedAt;

    public ReconciliationReportResponse() {
    }

    public ReconciliationReportResponse(BigDecimal totalDeposits, BigDecimal totalWithdrawals,
                                        BigDecimal expectedSystemReserve, BigDecimal actualAccountsBalanceSum,
                                        BigDecimal discrepancy, String reconciliationStatus,
                                        long totalLedgerEntriesCount, LocalDateTime generatedAt) {
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.expectedSystemReserve = expectedSystemReserve;
        this.actualAccountsBalanceSum = actualAccountsBalanceSum;
        this.discrepancy = discrepancy;
        this.reconciliationStatus = reconciliationStatus;
        this.totalLedgerEntriesCount = totalLedgerEntriesCount;
        this.generatedAt = generatedAt;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public BigDecimal getExpectedSystemReserve() {
        return expectedSystemReserve;
    }

    public BigDecimal getActualAccountsBalanceSum() {
        return actualAccountsBalanceSum;
    }

    public BigDecimal getDiscrepancy() {
        return discrepancy;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public long getTotalLedgerEntriesCount() {
        return totalLedgerEntriesCount;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
