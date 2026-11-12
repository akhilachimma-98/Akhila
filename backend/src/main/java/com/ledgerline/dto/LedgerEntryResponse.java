package com.ledgerline.dto;

import com.ledgerline.enums.LedgerEntryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single line item in an account passbook / audit ledger statement.
 */
public class LedgerEntryResponse {

    private Long id;
    private String transactionReference;
    private String accountNumber;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(Long id, String transactionReference, String accountNumber,
                               LedgerEntryType entryType, BigDecimal amount,
                               BigDecimal balanceAfter, String description,
                               LocalDateTime createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
