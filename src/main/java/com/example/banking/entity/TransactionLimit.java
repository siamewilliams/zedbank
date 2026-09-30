package com.example.banking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_limits",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "limit_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "limit_date", nullable = false)
    private LocalDate limitDate;

    @Column(name = "total_debited", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebited = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;
}