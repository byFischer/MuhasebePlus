package com.MuhasebePlus.demo.invoice.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.tax.entity.VatExemptionCode;
import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_vat_summary",
        uniqueConstraints = @UniqueConstraint(name = "uq_vat_summary_invoice_rate",
                columnNames = {"invoice_id", "vat_rate"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceVatSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", insertable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_base_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatBaseAmount;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "withholding_base_amount", precision = 15, scale = 2)
    private BigDecimal withholdingBaseAmount;

    @Column(name = "withholding_amount", precision = 15, scale = 2)
    private BigDecimal withholdingAmount;

    @Column(name = "withholding_tax_code_id")
    private Integer withholdingTaxCodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withholding_tax_code_id", insertable = false, updatable = false)
    private WithholdingTaxCode withholdingTaxCode;

    @Column(name = "exemption_amount", precision = 15, scale = 2)
    private BigDecimal exemptionAmount;

    @Column(name = "vat_exemption_code_id")
    private Integer vatExemptionCodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_exemption_code_id", insertable = false, updatable = false)
    private VatExemptionCode vatExemptionCode;
}
