package com.ledgerline.dto;

import com.ledgerline.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for Admin to change a user's lifecycle state (e.g. SUSPENDED, ACTIVE).
 */
public class UpdateUserStatusRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;

    private String reason;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(UserStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
