package com.MuhasebePlus.demo.vat.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vat_period")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VatPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "total_output_vat", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalOutputVat = BigDecimal.ZERO;

    @Column(name = "total_input_vat", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInputVat = BigDecimal.ZERO;

    @Column(name = "net_payable_vat", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netPayableVat = BigDecimal.ZERO;

    @Column(name = "previous_carried_forward_vat", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal previousCarriedForwardVat = BigDecimal.ZERO;

    @Column(name = "carried_forward_vat", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal carriedForwardVat = BigDecimal.ZERO;

    @Column(name = "settlement_journal_entry_id")
    private Long settlementJournalEntryId;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vatPeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VatDeclarationLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
