package com.MuhasebePlus.demo.cheque.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "cheque_movement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ChequeMovement extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Long movementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "cheque_id", nullable = false)
    private Long chequeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cheque_id", insertable = false, updatable = false)
    private Cheque cheque;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private ChequeMovementType movementType;

    @Column(name = "source_type", nullable = false, length = 20)
    @Builder.Default
    private String sourceType = "MANUAL";

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "reason", length = 500)
    private String reason;
}
