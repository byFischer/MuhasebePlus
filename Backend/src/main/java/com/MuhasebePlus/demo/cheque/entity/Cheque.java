package com.MuhasebePlus.demo.cheque.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.financial.entity.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cheque")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Cheque extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cheque_id")
    private Long chequeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "cheque_number", nullable = false, length = 50)
    private String chequeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "cheque_type", nullable = false, length = 20)
    private ChequeType chequeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cheque_kind", nullable = false, length = 20)
    private ChequeKind chequeKind;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "drawer_bank", length = 100)
    private String drawerBank;

    @Column(name = "drawer_branch", length = 100)
    private String drawerBranch;

    @Column(name = "drawer_account_number", length = 50)
    private String drawerAccountNumber;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.TRY;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ChequeStatus status = ChequeStatus.IN_PORTFOLIO;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "invoice_payment_id")
    private Long invoicePaymentId;

    @Column(name = "endorsed_to_customer_id")
    private Long endorsedToCustomerId;

    @Column(name = "notes", length = 500)
    private String notes;
}
