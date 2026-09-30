package com.example.banking.service;

import com.example.banking.entity.Account;
import com.example.banking.entity.TransactionLimit;
import com.example.banking.exception.BusinessException;
import com.example.banking.repository.TransactionLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LimitService {

    private final TransactionLimitRepository repository;

    @Value("${banking.limits.daily-per-account:100000}")
    private BigDecimal dailyPerAccount;

    @Value("${banking.limits.single-transfer-max:50000}")
    private BigDecimal singleTransferMax;

    @Transactional
    public void checkAndReserve(Account account, BigDecimal amount) {
        if (amount.compareTo(singleTransferMax) > 0) {
            throw new BusinessException("Amount exceeds single transaction max of " + singleTransferMax);
        }

        LocalDate today = LocalDate.now();
        TransactionLimit limit = repository.findByAccountAndDateForUpdate(account.getId(), today)
            .orElseGet(() -> repository.save(TransactionLimit.builder()
                .account(account).limitDate(today)
                .totalDebited(BigDecimal.ZERO).transactionCount(0).build()));

        BigDecimal newTotal = limit.getTotalDebited().add(amount);
        if (newTotal.compareTo(dailyPerAccount) > 0) {
            throw new BusinessException(
                "Daily limit exceeded. Remaining: " + dailyPerAccount.subtract(limit.getTotalDebited()));
        }

        limit.setTotalDebited(newTotal);
        limit.setTransactionCount(limit.getTransactionCount() + 1);
        repository.save(limit);
    }

    @Transactional
    public void release(Account account, BigDecimal amount) {
        repository.findByAccountAndDateForUpdate(account.getId(), LocalDate.now()).ifPresent(limit -> {
            limit.setTotalDebited(limit.getTotalDebited().subtract(amount).max(BigDecimal.ZERO));
            limit.setTransactionCount(Math.max(0, limit.getTransactionCount() - 1));
            repository.save(limit);
        });
    }
}