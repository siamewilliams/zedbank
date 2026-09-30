package com.example.banking.service;

import com.example.banking.dto.request.DepositRequest;
import com.example.banking.dto.request.ReversalRequest;
import com.example.banking.dto.request.TransferRequest;
import com.example.banking.dto.response.TransactionResponse;
import com.example.banking.entity.*;
import com.example.banking.event.TransactionCompletedEvent;
import com.example.banking.exception.BusinessException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.LedgerEntryRepository;
import com.example.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LimitService limitService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse deposit(DepositRequest req, String username, String ip) {
        Account account = accountRepository.findByAccountNumber(req.getAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        ensureActive(account);

        Transaction txn = createTransaction(TransactionType.DEPOSIT, req.getAmount(),
            account.getCurrency(),
            req.getDescription() != null ? req.getDescription() : "Cash deposit", username);
        transactionRepository.save(txn);

        account.setBalance(account.getBalance().add(req.getAmount()));
        accountRepository.save(account);
        writeLedger(txn, account, EntryType.CREDIT, req.getAmount());

        complete(txn, username, ip, null);
        return TransactionResponse.from(txn);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest req, String username, String ip) {
        if (req.getFromAccountNumber().equals(req.getToAccountNumber()))
            throw new BusinessException("Source and destination accounts must differ");

        Account from = accountRepository.findByAccountNumber(req.getFromAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account to = accountRepository.findByAccountNumber(req.getToAccountNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        Long firstId = Math.min(from.getId(), to.getId());
        Long secondId = Math.max(from.getId(), to.getId());
        accountRepository.findByIdForUpdate(firstId);
        accountRepository.findByIdForUpdate(secondId);

        ensureActive(from);
        ensureActive(to);

        limitService.checkAndReserve(from, req.getAmount());

        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            limitService.release(from, req.getAmount());
            throw new BusinessException("Insufficient funds");
        }

        Transaction txn = createTransaction(TransactionType.TRANSFER, req.getAmount(),
            from.getCurrency(),
            req.getDescription() != null ? req.getDescription() : "Internal transfer", username);
        transactionRepository.save(txn);

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        accountRepository.save(from);
        writeLedger(txn, from, EntryType.DEBIT, req.getAmount());

        to.setBalance(to.getBalance().add(req.getAmount()));
        accountRepository.save(to);
        writeLedger(txn, to, EntryType.CREDIT, req.getAmount());

        complete(txn, username, ip, null);
        return TransactionResponse.from(txn);
    }

    @Transactional
    public TransactionResponse reverse(String reference, ReversalRequest req, String username, String ip) {
        Transaction original = transactionRepository.findByReference(reference)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (original.getStatus() == TransactionStatus.REVERSED)
            throw new BusinessException("Transaction already reversed");
        if (original.getStatus() != TransactionStatus.COMPLETED)
            throw new BusinessException("Only completed transactions can be reversed");

        Transaction reversal = createTransaction(TransactionType.REVERSAL, original.getAmount(),
            original.getCurrency(),
            "Reversal of " + original.getReference() + " (" + req.getReason() + ")", username);
        transactionRepository.save(reversal);

        for (LedgerEntry entry : original.getLedgerEntries()) {
            Account acc = accountRepository.findByIdForUpdate(entry.getAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            EntryType flipped = entry.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;
            if (flipped == EntryType.CREDIT) {
                acc.setBalance(acc.getBalance().add(entry.getAmount()));
            } else {
                if (acc.getBalance().compareTo(entry.getAmount()) < 0)
                    throw new BusinessException("Cannot reverse — insufficient funds in " + acc.getAccountNumber());
                acc.setBalance(acc.getBalance().subtract(entry.getAmount()));
            }
            accountRepository.save(acc);
            writeLedger(reversal, acc, flipped, entry.getAmount());
        }

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        complete(reversal, username, ip, null);
        auditService.record(username, "REVERSAL", "Transaction",
            String.valueOf(original.getId()), original, reversal, ip);
        return TransactionResponse.from(reversal);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(String username, Pageable pageable) {
        return transactionRepository.findByCreatedBy(username, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> allHistory(Pageable pageable) {
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        return transactionRepository.findByReference(reference)
            .map(TransactionResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Transaction createTransaction(TransactionType type, BigDecimal amount,
                                          String currency, String description, String by) {
        return Transaction.builder()
            .reference(generateReference())
            .type(type)
            .status(TransactionStatus.PENDING)
            .amount(amount)
            .currency(currency)
            .description(description)
            .createdBy(by)
            .build();
    }

    private void complete(Transaction txn, String actor, String ip, Object before) {
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(txn);

        auditService.record(actor, txn.getType().name(), "Transaction",
            txn.getReference(), before, txn, ip);

        eventPublisher.publishEvent(new TransactionCompletedEvent(
            txn.getId(), txn.getReference(), txn.getType().name(),
            txn.getAmount(), txn.getCurrency(), txn.getCreatedBy(), txn.getCompletedAt()));
    }

    private void writeLedger(Transaction txn, Account acc, EntryType type, BigDecimal amount) {
        ledgerEntryRepository.save(LedgerEntry.builder()
            .transaction(txn).account(acc).entryType(type)
            .amount(amount).balanceAfter(acc.getBalance()).build());
    }

    private void ensureActive(Account a) {
        if (a.getStatus() != AccountStatus.ACTIVE)
            throw new BusinessException("Account " + a.getAccountNumber() + " is " + a.getStatus());
    }

    private String generateReference() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "TXN-" + date + "-" + rand;
    }
}