package com.MuhasebePlus.demo.fixedasset.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asset_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "depreciation_method", nullable = false, length = 30)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE";

    @Column(name = "useful_life_months", nullable = false)
    @Builder.Default
    private Integer usefulLifeMonths = 60;

    @Column(name = "annual_rate", precision = 5, scale = 2)
    private java.math.BigDecimal annualRate;

    @Column(name = "asset_account_code", length = 20)
    private String assetAccountCode;

    @Column(name = "depreciation_account_code", length = 20)
    private String depreciationAccountCode;
}
