package com.MuhasebePlus.demo.tax.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vat_exemption_code")
public class VatExemptionCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Integer codeId;

    @Column(name = "code", nullable = false, length = 10, unique = true)
    private String code;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private VatExemptionCategory category;

    @Column(name = "kdv_beyannamesi_satiri", length = 10)
    private String kdvBeyannamesiSatiri;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
