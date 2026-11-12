package com.ledgerline.dto;

import com.ledgerline.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for Admin to change an account's operational state (e.g. FROZEN, ACTIVE).
 */
public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus status;

    private String reason;

    public UpdateAccountStatusRequest() {
    }

    public UpdateAccountStatusRequest(AccountStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
