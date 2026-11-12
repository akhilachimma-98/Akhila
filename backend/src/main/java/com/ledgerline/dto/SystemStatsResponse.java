package com.ledgerline.dto;

import java.math.BigDecimal;

/**
 * High-level system metrics and financial totals for Admin/Auditor dashboards.
 */
public class SystemStatsResponse {

    private long totalUsers;
    private long totalCustomers;
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private BigDecimal totalSystemDeposits;
    private BigDecimal totalSystemWithdrawals;
    private BigDecimal totalTransfersVolume;
    private BigDecimal totalActiveBalance;
    private long totalTransactionsCount;

    public SystemStatsResponse() {
    }

    public SystemStatsResponse(long totalUsers, long totalCustomers, long totalAccounts,
                               long activeAccounts, long frozenAccounts,
                               BigDecimal totalSystemDeposits, BigDecimal totalSystemWithdrawals,
                               BigDecimal totalTransfersVolume, BigDecimal totalActiveBalance,
                               long totalTransactionsCount) {
        this.totalUsers = totalUsers;
        this.totalCustomers = totalCustomers;
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.frozenAccounts = frozenAccounts;
        this.totalSystemDeposits = totalSystemDeposits;
        this.totalSystemWithdrawals = totalSystemWithdrawals;
        this.totalTransfersVolume = totalTransfersVolume;
        this.totalActiveBalance = totalActiveBalance;
        this.totalTransactionsCount = totalTransactionsCount;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public long getFrozenAccounts() {
        return frozenAccounts;
    }

    public BigDecimal getTotalSystemDeposits() {
        return totalSystemDeposits;
    }

    public BigDecimal getTotalSystemWithdrawals() {
        return totalSystemWithdrawals;
    }

    public BigDecimal getTotalTransfersVolume() {
        return totalTransfersVolume;
    }

    public BigDecimal getTotalActiveBalance() {
        return totalActiveBalance;
    }

    public long getTotalTransactionsCount() {
        return totalTransactionsCount;
    }
}
