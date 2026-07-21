package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "withholding_declaration",
        uniqueConstraints = @UniqueConstraint(name = "uq_withholding_declaration",
                columnNames = {"company_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class WithholdingDeclaration extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @Column(name = "total_withholding_base", precision = 15, scale = 2)
    private BigDecimal totalWithholdingBase;

    @Column(name = "total_withholding_amount", precision = 15, scale = 2)
    private BigDecimal totalWithholdingAmount;

    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    @Builder.Default
    private List<WithholdingDeclarationLine> lines = new ArrayList<>();
}
