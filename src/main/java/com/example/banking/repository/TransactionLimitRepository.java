package com.example.banking.repository;

import com.example.banking.entity.TransactionLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM TransactionLimit l WHERE l.account.id = :accountId AND l.limitDate = :date")
    Optional<TransactionLimit> findByAccountAndDateForUpdate(@Param("accountId") Long accountId,
                                                             @Param("date") LocalDate date);
}