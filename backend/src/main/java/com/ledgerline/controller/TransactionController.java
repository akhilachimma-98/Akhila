package com.ledgerline.controller;

import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.LedgerEntryResponse;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for financial transactions:
 * Deposits, Withdrawals, Transfers, and Audit statements.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Executes account-to-account transfer with idempotency & concurrency protection.
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Header takes precedence if supplied
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }

        TransactionResponse response = transactionService.transferFunds(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", response));
    }

    /**
     * Executes atomic fund deposit.
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionResponse response = transactionService.depositFunds(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", response));
    }

    /**
     * Executes atomic fund withdrawal.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionResponse response = transactionService.withdrawFunds(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", response));
    }

    /**
     * Returns paginated transaction history for the specified account.
     */
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> history = transactionService.getAccountTransactions(accountNumber, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved", history));
    }

    /**
     * Returns immutable passbook / ledger entries with running balance for the specified account.
     */
    @GetMapping("/statement/{accountNumber}")
    public ResponseEntity<ApiResponse<Page<LedgerEntryResponse>>> getStatement(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<LedgerEntryResponse> statement = transactionService.getAccountLedgerStatement(accountNumber, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Account statement retrieved", statement));
    }
}
