package com.MuhasebePlus.demo.company.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "tax_office", length = 255)
    private String taxOffice;

    @Column(name = "tax_number", length = 20, unique = true)
    private String taxNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "tax_office_code", length = 10)
    private String taxOfficeCode;

    @Column(name = "trade_registry_no", length = 30)
    private String tradeRegistryNo;

    @Column(name = "mersis_no", length = 20)
    private String mersisNo;

    @Column(name = "nace", length = 10)
    private String nace;

    @Column(name = "declaration_period_type", length = 20)
    @Builder.Default
    private String declarationPeriodType = "MONTHLY";

    @Column(name = "corporate_tax_type", length = 30)
    @Builder.Default
    private String corporateTaxType = "GELIR_VERGISI";
}
