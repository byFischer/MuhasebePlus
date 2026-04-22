// Invoice.java
package com.MuhasebePlus.demo.invoice.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number", length = 50, unique = true)
    private String invoiceNumber;

    @Column(name = "customer_id")
    private Long customerId;

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
