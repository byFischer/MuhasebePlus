package com.MuhasebePlus.demo.report.controller;

import com.MuhasebePlus.demo.report.dto.response.ReconciliationResponseDto;
import com.MuhasebePlus.demo.report.dto.response.StatementResponseDto;
import com.MuhasebePlus.demo.report.service.PdfStatementService;
import com.MuhasebePlus.demo.report.service.ReconciliationService;
import com.MuhasebePlus.demo.report.service.StatementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementControllerUnitTest {

    @Mock
    private StatementService statementService;
    @Mock
    private ReconciliationService reconciliationService;
    @Mock
    private PdfStatementService pdfStatementService;

    @InjectMocks
    private StatementController controller;

    @Test
    void getStatement_delegatesToStatementService() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        StatementResponseDto response = statementResponse(start, end);
        when(statementService.generateStatement(7L, start, end)).thenReturn(response);

        ResponseEntity<StatementResponseDto> result = controller.getStatement(7L, start, end);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void getReconciliation_delegatesToReconciliationService() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        ReconciliationResponseDto response = reconciliationResponse(start, end);
        when(reconciliationService.generateReconciliation(7L, start, end)).thenReturn(response);

        ResponseEntity<ReconciliationResponseDto> result = controller.getReconciliation(7L, start, end);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void downloadStatementPdf_setsInlinePdfHeaders() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        byte[] bytes = new byte[] {37, 80, 68, 70};
        when(pdfStatementService.generatePdf(7L, start, end)).thenReturn(bytes);

        ResponseEntity<byte[]> result = controller.downloadStatementPdf(7L, start, end);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getFirst("Content-Type")).isEqualTo("application/pdf");
        assertThat(result.getHeaders().getFirst("Content-Disposition")).isEqualTo("inline; filename=\"cari-ekstre-7.pdf\"");
        assertThat(result.getBody()).isEqualTo(bytes);
    }

    private StatementResponseDto statementResponse(LocalDate start, LocalDate end) {
        return new StatementResponseDto(
                7L,
                "Ada Ltd.",
                "1234567890",
                "120.01.001",
                start,
                end,
                new BigDecimal("100.00"),
                new BigDecimal("250.00"),
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                List.of()
        );
    }

    private ReconciliationResponseDto reconciliationResponse(LocalDate start, LocalDate end) {
        return new ReconciliationResponseDto(
                7L,
                "Ada Ltd.",
                "1234567890",
                "Test A.S.",
                "9876543210",
                start,
                end,
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("200.00"),
                List.of()
        );
    }
}
