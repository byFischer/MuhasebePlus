package com.MuhasebePlus.demo.invoice.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoice_series")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class InvoiceSeries extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "series_id")
    private Long seriesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "series_code", length = 10, nullable = false)
    private String seriesCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    private InvoiceType invoiceType;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Column(name = "year")
    private Integer year;

    @Column(name = "last_sequence_number", nullable = false)
    private Long lastSequenceNumber;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
