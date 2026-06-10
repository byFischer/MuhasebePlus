package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vat_declaration",
        uniqueConstraints = @UniqueConstraint(name = "uq_vat_declaration_company_type_period",
                columnNames = {"company_id", "declaration_type", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class VatDeclaration extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "declaration_type", nullable = false, length = 10)
    private String declarationType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @Column(name = "total_sales_base", precision = 15, scale = 2)
    private BigDecimal totalSalesBase;

    @Column(name = "total_sales_vat", precision = 15, scale = 2)
    private BigDecimal totalSalesVat;

    @Column(name = "total_purchases_base", precision = 15, scale = 2)
    private BigDecimal totalPurchasesBase;

    @Column(name = "total_purchases_vat", precision = 15, scale = 2)
    private BigDecimal totalPurchasesVat;

    @Column(name = "previous_period_carryover", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal previousPeriodCarryover = BigDecimal.ZERO;

    @Column(name = "payable_vat", precision = 15, scale = 2)
    private BigDecimal payableVat;

    @Column(name = "next_period_carryover", precision = 15, scale = 2)
    private BigDecimal nextPeriodCarryover;

    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    @Builder.Default
    private List<VatDeclarationLine> lines = new ArrayList<>();
}
