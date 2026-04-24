// Invoice.java
package com.MuhasebePlus.demo.invoice.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.customer.entity.Customer;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "invoice_number", length = 50, unique = true)
    private String invoiceNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING) 
    private InvoiceType invoiceType;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING) 
    private PaymentStatus paymentStatus;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

}
