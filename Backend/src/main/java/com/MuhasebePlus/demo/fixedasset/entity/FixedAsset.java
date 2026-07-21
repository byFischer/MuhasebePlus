package com.MuhasebePlus.demo.fixedasset.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fixed_asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "salvage_value", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "net_book_value", precision = 15, scale = 2)
    private BigDecimal netBookValue;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "disposed_at")
    private LocalDate disposedAt;
}
