package com.example.banking.service;

import com.example.banking.dto.response.StatementResponse;
import com.example.banking.entity.Account;
import com.example.banking.entity.EntryType;
import com.example.banking.entity.LedgerEntry;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public StatementResponse generate(String accountNumber, LocalDateTime from, LocalDateTime to) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<LedgerEntry> entries = ledgerEntryRepository
            .findByAccountIdAndCreatedAtBetweenOrderByCreatedAtAsc(account.getId(), from, to);

        BigDecimal opening;
        if (entries.isEmpty()) {
            opening = account.getBalance();
        } else {
            LedgerEntry first = entries.get(0);
            opening = first.getEntryType() == EntryType.CREDIT
                ? first.getBalanceAfter().subtract(first.getAmount())
                : first.getBalanceAfter().add(first.getAmount());
        }

        List<StatementResponse.StatementLine> lines = new ArrayList<>();
        for (LedgerEntry e : entries) {
            BigDecimal debit = e.getEntryType() == EntryType.DEBIT ? e.getAmount() : null;
            BigDecimal credit = e.getEntryType() == EntryType.CREDIT ? e.getAmount() : null;
            lines.add(new StatementResponse.StatementLine(
                e.getCreatedAt(),
                e.getTransaction().getReference(),
                e.getTransaction().getDescription(),
                e.getTransaction().getType().name(),
                debit, credit, e.getBalanceAfter()));
        }

        BigDecimal closing = entries.isEmpty() ? opening
            : entries.get(entries.size() - 1).getBalanceAfter();

        return new StatementResponse(
            account.getAccountNumber(),
            account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName(),
            account.getCurrency(),
            from, to, opening, closing, lines);
    }
}