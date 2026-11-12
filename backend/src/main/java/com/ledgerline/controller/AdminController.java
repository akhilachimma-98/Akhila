package com.ledgerline.controller;

import com.ledgerline.dto.AdminUserSummaryDto;
import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.SystemStatsResponse;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.UpdateAccountStatusRequest;
import com.ledgerline.dto.UpdateUserStatusRequest;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing administrative management endpoints.
 * Strictly restricted to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Lists all registered users and their linked accounts.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryDto>>> getAllUsers(
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AdminUserSummaryDto> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    /**
     * Updates an account's status (e.g. FREEZE, ACTIVE, CLOSED).
     */
    @PatchMapping("/accounts/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto.AccountDetailDto>> updateAccountStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {

        AdminUserSummaryDto.AccountDetailDto updated = adminService.updateAccountStatus(
                accountNumber,
                request,
                adminUser.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.success("Account status updated to " + request.getStatus(), updated));
    }

    /**
     * Updates a user's status (e.g. SUSPENDED, ACTIVE).
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {

        adminService.updateUserStatus(userId, request, adminUser.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User status updated to " + request.getStatus(), null));
    }

    /**
     * Retrieves overall system financial health and operational counts.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getSystemStats() {
        SystemStatsResponse stats = adminService.getSystemStatistics();
        return ResponseEntity.ok(ApiResponse.success("System statistics retrieved", stats));
    }

    /**
     * Retrieves all system transactions with optional status filter.
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> transactions = adminService.getAllTransactions(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("System transactions retrieved", transactions));
    }
}
