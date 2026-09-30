package com.example.banking.repository;

import com.example.banking.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}