package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vat_declaration_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VatDeclarationLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private VatDeclaration declaration;

    @Column(name = "line_code", nullable = false, length = 10)
    private String lineCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "line_order", nullable = false)
    private Integer lineOrder;
}
