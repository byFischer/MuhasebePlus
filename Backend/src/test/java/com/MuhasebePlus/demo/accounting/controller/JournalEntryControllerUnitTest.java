package com.MuhasebePlus.demo.accounting.controller;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryLineRequestDto;
import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.GeneralLedgerRowDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryControllerUnitTest {

    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private JournalEntryController controller;

    @Test
    void list_returnsServicePage() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<JournalEntryResponseDto> page = new PageImpl<>(List.of(entryDto(1L)));
        when(journalEntryService.list(pageable)).thenReturn(page);

        ResponseEntity<Page<JournalEntryResponseDto>> response = controller.list(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void getById_returnsServiceEntry() {
        JournalEntryResponseDto dto = entryDto(10L);
        when(journalEntryService.getById(10L)).thenReturn(dto);

        ResponseEntity<JournalEntryResponseDto> response = controller.getById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void createManual_returnsCreatedEntry() {
        JournalEntryRequestDto request = request();
        JournalEntryResponseDto dto = entryDto(11L);
        when(journalEntryService.createManualEntry(request)).thenReturn(dto);

        ResponseEntity<JournalEntryResponseDto> response = controller.createManual(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void reverse_returnsReversalEntry() {
        JournalEntryResponseDto dto = entryDto(12L);
        when(journalEntryService.reverseEntry(10L, "Yanlis fis")).thenReturn(dto);

        ResponseEntity<JournalEntryResponseDto> response = controller.reverse(10L, "Yanlis fis");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void delete_returnsNoContent() {
        ResponseEntity<Void> response = controller.delete(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(journalEntryService).delete(10L);
    }

    @Test
    void trialBalance_returnsRows() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        TrialBalanceRowDto row = new TrialBalanceRowDto(
                100L, "100", "Kasa", AccountType.ASSET,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        when(journalEntryService.getTrialBalance(start, end)).thenReturn(List.of(row));

        ResponseEntity<List<TrialBalanceRowDto>> response = controller.trialBalance(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(row);
    }

    @Test
    void incomeStatement_returnsStatement() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        IncomeStatementResponseDto statement = new IncomeStatementResponseDto(
                start, end, new BigDecimal("1000.00"), new BigDecimal("400.00"),
                new BigDecimal("600.00"), new BigDecimal("60.00"), List.of());
        when(journalEntryService.getIncomeStatement(start, end)).thenReturn(statement);

        ResponseEntity<IncomeStatementResponseDto> response = controller.incomeStatement(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(statement);
    }

    @Test
    void balanceSheet_returnsStatement() {
        LocalDate asOfDate = LocalDate.of(2026, 12, 31);
        BalanceSheetResponseDto statement = new BalanceSheetResponseDto(
                asOfDate, new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("800.00"), new BigDecimal("1000.00"), BigDecimal.ZERO,
                List.of(), List.of(), List.of());
        when(journalEntryService.getBalanceSheet(asOfDate)).thenReturn(statement);

        ResponseEntity<BalanceSheetResponseDto> response = controller.balanceSheet(asOfDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(statement);
    }

    @Test
    void generalLedger_returnsRows() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        GeneralLedgerRowDto row = new GeneralLedgerRowDto(
                1L, start, "FIS-2026-0001", "Acilis",
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        when(journalEntryService.getGeneralLedger(100L, start, end)).thenReturn(List.of(row));

        ResponseEntity<List<GeneralLedgerRowDto>> response = controller.generalLedger(100L, start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(row);
    }

    private JournalEntryRequestDto request() {
        return new JournalEntryRequestDto(
                LocalDate.of(2026, 5, 1),
                "Manuel fis",
                List.of(
                        new JournalEntryLineRequestDto(100L, new BigDecimal("100.00"), BigDecimal.ZERO, "Borc"),
                        new JournalEntryLineRequestDto(200L, BigDecimal.ZERO, new BigDecimal("100.00"), "Alacak")
                ));
    }

    private JournalEntryResponseDto entryDto(Long id) {
        return new JournalEntryResponseDto(
                id,
                "FIS-2026-0001",
                LocalDate.of(2026, 5, 1),
                "Manuel fis",
                JournalSourceType.MANUAL,
                null,
                false,
                null,
                List.of(),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }
}
