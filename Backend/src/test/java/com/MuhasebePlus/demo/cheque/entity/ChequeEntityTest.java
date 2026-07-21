package com.MuhasebePlus.demo.cheque.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.financial.entity.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChequeEntityTest {

    @Test
    void chequeDefaultsAllArgsAndSoftDeleteFieldsWork() {
        Company company = company();
        LocalDate issueDate = LocalDate.of(2026, 6, 1);
        LocalDate dueDate = LocalDate.of(2026, 7, 1);
        Cheque cheque = new Cheque(
                1L,
                company,
                "CHK-001",
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                20L,
                "Test Bankasi",
                "Merkez",
                "123456",
                new BigDecimal("1000.00"),
                Currency.TRY,
                issueDate,
                dueDate,
                ChequeStatus.IN_PORTFOLIO,
                10L,
                100L,
                200L,
                30L,
                "Not");
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        cheque.setDeleted(true);
        cheque.setDeletedAt(deletedAt);

        assertThat(cheque.getChequeId()).isEqualTo(1L);
        assertThat(cheque.getCompany()).isEqualTo(company);
        assertThat(cheque.getChequeNumber()).isEqualTo("CHK-001");
        assertThat(cheque.getChequeType()).isEqualTo(ChequeType.RECEIVABLE);
        assertThat(cheque.getChequeKind()).isEqualTo(ChequeKind.CHEQUE);
        assertThat(cheque.getCustomerId()).isEqualTo(20L);
        assertThat(cheque.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(cheque.getCurrency()).isEqualTo(Currency.TRY);
        assertThat(cheque.getStatus()).isEqualTo(ChequeStatus.IN_PORTFOLIO);
        assertThat(cheque.getBankAccountId()).isEqualTo(10L);
        assertThat(cheque.getTransactionId()).isEqualTo(100L);
        assertThat(cheque.getInvoicePaymentId()).isEqualTo(200L);
        assertThat(cheque.getEndorsedToCustomerId()).isEqualTo(30L);
        assertThat(cheque.isDeleted()).isTrue();
        assertThat(cheque.getDeletedAt()).isEqualTo(deletedAt);

        Cheque defaults = Cheque.builder()
                .company(company)
                .chequeNumber("CHK-DEF")
                .chequeType(ChequeType.PAYABLE)
                .chequeKind(ChequeKind.PROMISSORY_NOTE)
                .amount(BigDecimal.ONE)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .build();
        assertThat(defaults.getCurrency()).isEqualTo(Currency.TRY);
        assertThat(defaults.getStatus()).isEqualTo(ChequeStatus.IN_PORTFOLIO);
    }

    @Test
    void chequeMovementDefaultsAllArgsAndEnumsWork() {
        Company company = company();
        Cheque cheque = Cheque.builder()
                .chequeId(1L)
                .company(company)
                .chequeNumber("CHK-001")
                .chequeType(ChequeType.RECEIVABLE)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(BigDecimal.TEN)
                .issueDate(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 7, 1))
                .build();
        ChequeMovement movement = new ChequeMovement(
                5L,
                company,
                1L,
                cheque,
                ChequeMovementType.COLLECTED,
                "MANUAL",
                60L,
                LocalDate.of(2026, 6, 2),
                10L,
                100L,
                "Tahsil edildi");

        assertThat(movement.getMovementId()).isEqualTo(5L);
        assertThat(movement.getCompany()).isEqualTo(company);
        assertThat(movement.getChequeId()).isEqualTo(1L);
        assertThat(movement.getCheque()).isEqualTo(cheque);
        assertThat(movement.getMovementType()).isEqualTo(ChequeMovementType.COLLECTED);
        assertThat(movement.getSourceType()).isEqualTo("MANUAL");
        assertThat(movement.getSourceId()).isEqualTo(60L);
        assertThat(movement.getBankAccountId()).isEqualTo(10L);
        assertThat(movement.getTransactionId()).isEqualTo(100L);
        assertThat(movement.getReason()).isEqualTo("Tahsil edildi");

        ChequeMovement defaults = ChequeMovement.builder()
                .company(company)
                .chequeId(1L)
                .movementType(ChequeMovementType.REGISTERED)
                .movementDate(LocalDate.of(2026, 6, 1))
                .build();
        assertThat(defaults.getSourceType()).isEqualTo("MANUAL");
        assertThat(ChequeType.values()).containsExactly(ChequeType.RECEIVABLE, ChequeType.PAYABLE);
        assertThat(ChequeKind.values()).containsExactly(ChequeKind.CHEQUE, ChequeKind.PROMISSORY_NOTE);
        assertThat(ChequeStatus.values()).contains(
                ChequeStatus.IN_PORTFOLIO,
                ChequeStatus.DEPOSITED,
                ChequeStatus.COLLECTED,
                ChequeStatus.BOUNCED,
                ChequeStatus.ENDORSED,
                ChequeStatus.CANCELLED);
        assertThat(ChequeMovementType.values()).contains(
                ChequeMovementType.REGISTERED,
                ChequeMovementType.DEPOSITED,
                ChequeMovementType.COLLECTED,
                ChequeMovementType.BOUNCED,
                ChequeMovementType.ENDORSED,
                ChequeMovementType.CANCELLED);
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        return company;
    }
}
