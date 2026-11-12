package com.ledgerline.entity;

import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level business transaction record (Deposit, Withdrawal, Transfer).
 * Contains the business intent, idempotency reference, and lifecycle status.
 */
@Entity
@Table(
    name = "transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_reference", columnNames = {"transaction_reference"}),
        @UniqueConstraint(name = "uk_transactions_idempotency", columnNames = {"idempotency_key"})
    },
    indexes = {
        @Index(name = "idx_transactions_source_acc", columnList = "source_account_id"),
        @Index(name = "idx_transactions_target_acc", columnList = "target_account_id"),
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at")
    }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "transaction_reference", nullable = false, length = 64)
    private String transactionReference;

    @Column(name = "from_account", length = 20)
    private String fromAccount;

    @Column(name = "to_account", length = 20)
    private String toAccount;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "source_account_id",
        foreignKey = @ForeignKey(name = "fk_transactions_source_acc")
    )
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "target_account_id",
        foreignKey = @ForeignKey(name = "fk_transactions_target_acc")
    )
    private Account targetAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.transactionId == null || this.transactionId.isBlank()) {
            if (this.transactionReference != null && !this.transactionReference.isBlank()) {
                this.transactionId = this.transactionReference;
            } else {
                this.transactionId = "TXN-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            }
        }
        if (this.transactionReference == null || this.transactionReference.isBlank()) {
            this.transactionReference = this.transactionId;
        }
        if (this.fromAccount == null && this.sourceAccount != null) {
            this.fromAccount = this.sourceAccount.getAccountNumber();
        }
        if (this.toAccount == null && this.targetAccount != null) {
            this.toAccount = this.targetAccount.getAccountNumber();
        }
    }

    public Transaction() {
    }

    public Transaction(String transactionReference, String idempotencyKey, Account sourceAccount,
                       Account targetAccount, BigDecimal amount, TransactionType type, String description) {
        String generatedRef = (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference
                : "TXN-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        this.transactionReference = generatedRef;
        this.transactionId = generatedRef;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.fromAccount = sourceAccount != null ? sourceAccount.getAccountNumber() : null;
        this.toAccount = targetAccount != null ? targetAccount.getAccountNumber() : null;
        this.amount = amount != null ? amount.setScale(2) : BigDecimal.ZERO.setScale(2);
        this.type = type;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        if (this.transactionReference == null || this.transactionReference.isBlank()) {
            this.transactionReference = transactionId;
        }
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
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

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2) : BigDecimal.ZERO.setScale(2);
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<LedgerEntry> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }

    public void addLedgerEntry(LedgerEntry entry) {
        ledgerEntries.add(entry);
        entry.setTransaction(this);
    }
}
