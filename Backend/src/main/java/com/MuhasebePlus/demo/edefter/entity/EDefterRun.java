package com.MuhasebePlus.demo.edefter.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "edefter_run",
        uniqueConstraints = @UniqueConstraint(name = "uq_edefter_run",
                columnNames = {"company_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class EDefterRun extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "journal_xml", columnDefinition = "TEXT")
    private String journalXml;

    @Column(name = "ledger_xml", columnDefinition = "TEXT")
    private String ledgerXml;

    @Column(name = "journal_entry_count")
    private Integer journalEntryCount;

    @Column(name = "journal_line_count")
    private Integer journalLineCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EDefterStatus status = EDefterStatus.GENERATED;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
}
