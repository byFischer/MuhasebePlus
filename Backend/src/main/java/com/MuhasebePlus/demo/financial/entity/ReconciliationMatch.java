package com.MuhasebePlus.demo.financial.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_match")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconciliationMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_line_id", nullable = false)
    private BankStatementLine statementLine;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Builder.Default
    @Column(name = "match_type", nullable = false, length = 10)
    private String matchType = "MANUAL";

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @PrePersist
    protected void onCreate() {
        if (matchedAt == null) matchedAt = LocalDateTime.now();
    }
}
