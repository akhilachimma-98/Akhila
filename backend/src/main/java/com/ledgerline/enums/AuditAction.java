package com.ledgerline.enums;

/**
 * Categorization of security and financial audit events.
 */
public enum AuditAction {
    USER_REGISTERED,
    USER_STATUS_UPDATED,
    ACCOUNT_CREATED,
    ACCOUNT_STATUS_UPDATED,
    DEPOSIT_COMPLETED,
    WITHDRAWAL_COMPLETED,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED,
    SECURITY_ACCESS_DENIED
}
