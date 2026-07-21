package com.MuhasebePlus.demo.financial.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bank_statement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "statement_date")
    private LocalDate statementDate;

    @Builder.Default
    @Column(name = "opening_balance", precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "closing_balance", precision = 15, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "import_date")
    private LocalDateTime importDate;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bankStatement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BankStatementLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (importDate == null) importDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
