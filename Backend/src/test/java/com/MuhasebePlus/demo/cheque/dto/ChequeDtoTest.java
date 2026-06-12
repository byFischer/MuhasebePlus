package com.MuhasebePlus.demo.cheque.dto;

import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeDetailsDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeMovementResponseDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequePortfolioSummaryDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeResponseDto;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeMovementType;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChequeDtoTest {

    @Test
    void requestRecordsExposeConstructorValues() {
        LocalDate issueDate = LocalDate.of(2026, 6, 1);
        LocalDate dueDate = LocalDate.of(2026, 7, 1);

        ChequeRequestDto request = new ChequeRequestDto(
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
                "Not");
        ChequeDetailsDto details = new ChequeDetailsDto(
                "CHK-002",
                ChequeKind.PROMISSORY_NOTE,
                "Diger Banka",
                "Sube",
                "654321",
                issueDate,
                dueDate);

        assertThat(request.chequeNumber()).isEqualTo("CHK-001");
        assertThat(request.chequeType()).isEqualTo(ChequeType.RECEIVABLE);
        assertThat(request.chequeKind()).isEqualTo(ChequeKind.CHEQUE);
        assertThat(request.customerId()).isEqualTo(20L);
        assertThat(request.drawerBank()).isEqualTo("Test Bankasi");
        assertThat(request.amount()).isEqualByComparingTo("1000.00");
        assertThat(request.currency()).isEqualTo(Currency.TRY);
        assertThat(request.notes()).isEqualTo("Not");
        assertThat(details.chequeNumber()).isEqualTo("CHK-002");
        assertThat(details.chequeKind()).isEqualTo(ChequeKind.PROMISSORY_NOTE);
        assertThat(details.drawerAccountNumber()).isEqualTo("654321");
        assertThat(new CollectChequeRequestDto(10L).bankAccountId()).isEqualTo(10L);
        assertThat(new EndorseChequeRequestDto(30L).toCustomerId()).isEqualTo(30L);
        assertThat(new BounceReasonRequestDto("Karsiliksiz").reason()).isEqualTo("Karsiliksiz");
    }

    @Test
    void responseRecordsExposeConstructorValues() {
        LocalDate movementDate = LocalDate.of(2026, 6, 2);
        ChequeMovementResponseDto movement = new ChequeMovementResponseDto(
                5L,
                ChequeMovementType.COLLECTED,
                "MANUAL",
                movementDate,
                10L,
                100L,
                "Tahsil edildi");
        ChequeResponseDto response = new ChequeResponseDto(
                1L,
                "CHK-001",
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                20L,
                "ABC Musteri",
                "Test Bankasi",
                "Merkez",
                "123456",
                new BigDecimal("1000.00"),
                Currency.TRY,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1),
                ChequeStatus.COLLECTED,
                10L,
                100L,
                200L,
                30L,
                "Not",
                List.of(movement));
        ChequePortfolioSummaryDto summary = new ChequePortfolioSummaryDto(
                4, new BigDecimal("1000.00"),
                1, new BigDecimal("100.00"),
                1, new BigDecimal("200.00"),
                1, new BigDecimal("300.00"),
                1, new BigDecimal("400.00"),
                1, new BigDecimal("100.00"),
                1, new BigDecimal("200.00"),
                1, new BigDecimal("300.00"));

        assertThat(response.chequeId()).isEqualTo(1L);
        assertThat(response.chequeNumber()).isEqualTo("CHK-001");
        assertThat(response.customerName()).isEqualTo("ABC Musteri");
        assertThat(response.bankAccountId()).isEqualTo(10L);
        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.invoicePaymentId()).isEqualTo(200L);
        assertThat(response.endorsedToCustomerId()).isEqualTo(30L);
        assertThat(response.movements()).containsExactly(movement);
        assertThat(movement.movementType()).isEqualTo(ChequeMovementType.COLLECTED);
        assertThat(movement.movementDate()).isEqualTo(movementDate);
        assertThat(summary.totalCount()).isEqualTo(4);
        assertThat(summary.totalAmount()).isEqualByComparingTo("1000.00");
        assertThat(summary.due60PlusAmount()).isEqualByComparingTo("300.00");
    }
}
