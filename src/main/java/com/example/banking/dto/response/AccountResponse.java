package com.example.banking.dto.response;

import com.example.banking.entity.Account;
import com.example.banking.entity.AccountStatus;
import com.example.banking.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    Long id,
    String accountNumber,
    String customerName,
    AccountType accountType,
    AccountStatus status,
    BigDecimal balance,
    String currency,
    LocalDateTime createdAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
            a.getId(),
            a.getAccountNumber(),
            a.getCustomer().getFirstName() + " " + a.getCustomer().getLastName(),
            a.getAccountType(),
            a.getStatus(),
            a.getBalance(),
            a.getCurrency(),
            a.getCreatedAt()
        );
    }
}