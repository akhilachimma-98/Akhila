package com.ledgerline.controller;

import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.AuditLogResponse;
import com.ledgerline.dto.LedgerEntryResponse;
import com.ledgerline.dto.ReconciliationReportResponse;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing read-only auditing and reconciliation endpoints.
 * Accessible strictly by users with ROLE_AUDITOR or ROLE_ADMIN.
 * No state modifications can be performed through this controller.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Retrieves system-wide audit logs with optional action filter.
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> logs = auditService.getAuditLogs(action, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", logs));
    }

    /**
     * Retrieves system-wide double-entry ledger lines.
     */
    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<Page<LedgerEntryResponse>>> getLedger(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<LedgerEntryResponse> ledgerEntries = auditService.getAllLedgerEntries(pageable);
        return ResponseEntity.ok(ApiResponse.success("System ledger entries retrieved", ledgerEntries));
    }

    /**
     * Performs a live bank balance reconciliation verification.
     */
    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponse<ReconciliationReportResponse>> getReconciliationReport() {
        ReconciliationReportResponse report = auditService.performReconciliation();
        return ResponseEntity.ok(ApiResponse.success("Financial reconciliation report generated", report));
    }
}
