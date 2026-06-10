package com.MuhasebePlus.demo.vat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity(name = "VatPeriodLine")
@Table(name = "vat_period_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VatDeclarationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_period_id", nullable = false)
    private VatPeriod vatPeriod;

    @Column(name = "line_type", nullable = false, length = 10)
    private String lineType; // OUTPUT, INPUT

    @Column(name = "vat_rate", nullable = false)
    private Integer vatRate; // 1, 8, 10, 18, 20

    @Column(name = "tax_base", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxBase = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;
}
