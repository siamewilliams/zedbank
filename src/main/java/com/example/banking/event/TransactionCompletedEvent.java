package com.example.banking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCompletedEvent(
    Long transactionId,
    String reference,
    String type,
    BigDecimal amount,
    String currency,
    String createdBy,
    LocalDateTime completedAt
) {}