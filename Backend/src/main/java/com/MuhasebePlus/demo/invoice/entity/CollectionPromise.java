package com.MuhasebePlus.demo.invoice.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "collection_promise")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class CollectionPromise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promise_id")
    private Long promiseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "promised_date", nullable = false)
    private LocalDate promisedDate;

    @Column(name = "promised_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal promisedAmount;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean fulfilled;

    @Column(name = "fulfilled_at")
    private LocalDate fulfilledAt;
}
