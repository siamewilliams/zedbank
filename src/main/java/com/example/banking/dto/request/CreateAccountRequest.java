package com.example.banking.dto.request;

import com.example.banking.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotNull private Long customerId;
    @NotNull private AccountType accountType;
    private String currency = "ZMW";
}