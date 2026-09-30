package com.example.banking.service;

import com.example.banking.dto.request.CreateAccountRequest;
import com.example.banking.dto.response.AccountResponse;
import com.example.banking.entity.Account;
import com.example.banking.entity.AccountStatus;
import com.example.banking.entity.Customer;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Account account = Account.builder()
            .accountNumber(generateAccountNumber())
            .customer(customer)
            .accountType(req.getAccountType())
            .status(AccountStatus.ACTIVE)
            .balance(BigDecimal.ZERO)
            .currency(req.getCurrency() != null ? req.getCurrency() : "ZMW")
            .build();

        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .map(AccountResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
            .map(AccountResponse::from)
            .toList();
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = "10" + String.format("%08d", ThreadLocalRandom.current().nextInt(0, 100_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}