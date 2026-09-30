package com.example.banking.dto.response;

import com.example.banking.entity.Transaction;
import com.example.banking.entity.TransactionStatus;
import com.example.banking.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    String reference,
    TransactionType type,
    TransactionStatus status,
    BigDecimal amount,
    String currency,
    String description,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
            t.getId(), t.getReference(), t.getType(), t.getStatus(),
            t.getAmount(), t.getCurrency(), t.getDescription(),
            t.getCreatedBy(), t.getCreatedAt(), t.getCompletedAt()
        );
    }
}