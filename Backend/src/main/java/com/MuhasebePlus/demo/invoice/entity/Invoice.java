// Invoice.java
package com.MuhasebePlus.demo.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number", length = 50, unique = true)
    private String invoiceNumber;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "invoice_type")
    private String invoiceType;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_amount", precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;
}
