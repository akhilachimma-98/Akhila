package com.ledgerline.dto;

import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.UserRole;
import com.ledgerline.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary view of a user and their associated bank accounts for the Admin panel.
 */
public class AdminUserSummaryDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private List<AccountDetailDto> accounts;

    public static class AccountDetailDto {
        private Long id;
        private String accountNumber;
        private AccountType accountType;
        private BigDecimal balance;
        private String currency;
        private AccountStatus status;
        private Long version;
        private LocalDateTime createdAt;

        public AccountDetailDto() {
        }

        public AccountDetailDto(Long id, String accountNumber, AccountType accountType,
                                BigDecimal balance, String currency, AccountStatus status,
                                Long version, LocalDateTime createdAt) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.accountType = accountType;
            this.balance = balance;
            this.currency = currency;
            this.status = status;
            this.version = version;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public AccountType getAccountType() {
            return accountType;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public String getCurrency() {
            return currency;
        }

        public AccountStatus getStatus() {
            return status;
        }

        public Long getVersion() {
            return version;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public AdminUserSummaryDto() {
    }

    public AdminUserSummaryDto(Long id, String username, String email, String fullName,
                               String phoneNumber, UserRole role, UserStatus status,
                               LocalDateTime createdAt, List<AccountDetailDto> accounts) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.accounts = accounts;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<AccountDetailDto> getAccounts() {
        return accounts;
    }
}
