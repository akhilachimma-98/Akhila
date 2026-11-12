package com.ledgerline.dto;

import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Payload for registering a new user.
 */
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain alphanumeric characters, dots, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    private String phoneNumber;

    /**
     * Optional: Defaults to ROLE_CUSTOMER if not supplied.
     */
    private UserRole role = UserRole.ROLE_CUSTOMER;

    /**
     * Optional initial deposit for new customer accounts. Defaults to 0.00.
     */
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    /**
     * Optional account type: SAVINGS or CHECKING (defaults to SAVINGS).
     */
    private AccountType accountType = AccountType.SAVINGS;

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role != null ? role : UserRole.ROLE_CUSTOMER;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public AccountType getAccountType() {
        return accountType != null ? accountType : AccountType.SAVINGS;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
