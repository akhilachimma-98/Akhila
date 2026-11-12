package com.ledgerline.repository;

import com.ledgerline.entity.Transaction;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    /**
     * Finds transactions where an account is either the sender or the receiver.
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.type = 'DEPOSIT' AND t.status = 'COMPLETED'")
    java.math.BigDecimal calculateTotalDeposits();

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.type = 'WITHDRAWAL' AND t.status = 'COMPLETED'")
    java.math.BigDecimal calculateTotalWithdrawals();

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.type = 'TRANSFER' AND t.status = 'COMPLETED'")
    java.math.BigDecimal calculateTotalTransfers();
}
