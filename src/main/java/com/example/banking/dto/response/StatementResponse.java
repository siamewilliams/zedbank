package com.example.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StatementResponse(
    String accountNumber,
    String customerName,
    String currency,
    LocalDateTime from,
    LocalDateTime to,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<StatementLine> lines
) {
    public record StatementLine(
        LocalDateTime date,
        String reference,
        String description,
        String type,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance
    ) {}
}