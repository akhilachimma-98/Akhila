package com.ledgerline.dto;

import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standard response payload returned after executing or querying a transaction.
 */
public class TransactionResponse {

    private String transactionId;
    private String transactionReference;
    private String idempotencyKey;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public TransactionResponse() {
    }

    public TransactionResponse(String transactionReference, String idempotencyKey,
                               String sourceAccountNumber, String targetAccountNumber,
                               BigDecimal amount, TransactionType type, TransactionStatus status,
                               String description, BigDecimal balanceAfter,
                               LocalDateTime createdAt, LocalDateTime completedAt) {
        this.transactionReference = transactionReference;
        this.transactionId = transactionReference;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getTransactionId() {
        return transactionId != null ? transactionId : transactionReference;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        if (this.transactionReference == null) {
            this.transactionReference = transactionId;
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
