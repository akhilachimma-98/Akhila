package com.ledgerline.enums;

/**
 * Processing state of an idempotent request.
 */
public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
