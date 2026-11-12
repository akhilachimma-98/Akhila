package com.ledgerline.enums;

/**
 * Operational state of an account.
 * FROZEN accounts cannot transfer, deposit, or withdraw funds.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
