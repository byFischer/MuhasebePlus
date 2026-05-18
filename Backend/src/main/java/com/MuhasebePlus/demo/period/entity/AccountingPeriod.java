package com.MuhasebePlus.demo.period.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_period")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AccountingPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_id")
    private Long periodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private PeriodStatus status = PeriodStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by")
    private Long reopenedBy;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;
}
