package com.MuhasebePlus.demo.fixedasset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_line")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_asset_id", nullable = false)
    private FixedAsset fixedAsset;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "depreciation_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depreciationAmount;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 15, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(name = "net_book_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal netBookValue;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
