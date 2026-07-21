package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "withholding_declaration_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithholdingDeclarationLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private WithholdingDeclaration declaration;

    @Column(name = "withholding_tax_code_id")
    private Integer withholdingTaxCodeId;

    @Column(name = "withholding_code", length = 10)
    private String withholdingCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "withholding_amount", precision = 15, scale = 2)
    private BigDecimal withholdingAmount;

    @Column(name = "document_count")
    private Integer documentCount;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;
}
