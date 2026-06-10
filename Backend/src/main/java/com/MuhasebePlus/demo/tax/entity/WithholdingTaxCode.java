package com.MuhasebePlus.demo.tax.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "withholding_tax_code")
public class WithholdingTaxCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Integer codeId;

    @Column(name = "code", nullable = false, length = 10, unique = true)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "withholding_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal withholdingRate;

    @Column(name = "denominator", nullable = false)
    private Integer denominator;

    @Column(name = "numerator", nullable = false)
    private Integer numerator;

    @Column(name = "service_category", length = 100)
    private String serviceCategory;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
