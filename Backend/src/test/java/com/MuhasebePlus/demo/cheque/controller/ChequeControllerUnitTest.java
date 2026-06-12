package com.MuhasebePlus.demo.cheque.controller;

import com.MuhasebePlus.demo.cheque.dto.request.BounceReasonRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.ChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.CollectChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.request.EndorseChequeRequestDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequePortfolioSummaryDto;
import com.MuhasebePlus.demo.cheque.dto.response.ChequeResponseDto;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.cheque.service.ChequeService;
import com.MuhasebePlus.demo.financial.entity.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChequeControllerUnitTest {

    @Mock private ChequeService chequeService;

    @InjectMocks private ChequeController chequeController;

    @Test
    void createGetAndDeleteEndpoints_delegateToService() {
        ChequeRequestDto request = requestDto();
        ChequeResponseDto response = responseDto(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeService.createCheque(request)).thenReturn(response);
        when(chequeService.getChequeById(1L)).thenReturn(response);

        var createResponse = chequeController.createCheque(request);
        var getResponse = chequeController.getChequeById(1L);
        var deleteResponse = chequeController.deleteCheque(1L);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isEqualTo(response);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(response);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(chequeService).deleteCheque(1L);
    }

    @Test
    void listEndpoints_useStatusWhenProvidedOtherwiseReturnAll() {
        ChequeResponseDto inPortfolio = responseDto(1L, ChequeStatus.IN_PORTFOLIO);
        ChequeResponseDto deposited = responseDto(2L, ChequeStatus.DEPOSITED);
        when(chequeService.getAllCheques()).thenReturn(List.of(inPortfolio));
        when(chequeService.getChequesByStatus("deposited")).thenReturn(List.of(deposited));

        assertThat(chequeController.getAllCheques(null).getBody()).containsExactly(inPortfolio);
        assertThat(chequeController.getAllCheques(" ").getBody()).containsExactly(inPortfolio);
        assertThat(chequeController.getAllCheques("deposited").getBody()).containsExactly(deposited);

        verify(chequeService).getChequesByStatus("deposited");
    }

    @Test
    void statusTransitionEndpoints_delegateToService() {
        ChequeResponseDto deposited = responseDto(1L, ChequeStatus.DEPOSITED);
        ChequeResponseDto collected = responseDto(1L, ChequeStatus.COLLECTED);
        ChequeResponseDto bounced = responseDto(1L, ChequeStatus.BOUNCED);
        ChequeResponseDto endorsed = responseDto(1L, ChequeStatus.ENDORSED);
        ChequeResponseDto cancelled = responseDto(1L, ChequeStatus.CANCELLED);
        CollectChequeRequestDto collectRequest = new CollectChequeRequestDto(10L);
        BounceReasonRequestDto bounceRequest = new BounceReasonRequestDto("Karsiliksiz");
        EndorseChequeRequestDto endorseRequest = new EndorseChequeRequestDto(20L);
        when(chequeService.deposit(1L, 10L)).thenReturn(deposited);
        when(chequeService.markAsCollected(1L, collectRequest)).thenReturn(collected);
        when(chequeService.markAsBounced(1L, bounceRequest)).thenReturn(bounced);
        when(chequeService.endorse(1L, endorseRequest)).thenReturn(endorsed);
        when(chequeService.cancel(1L, "iptal")).thenReturn(cancelled);

        assertThat(chequeController.deposit(1L, 10L).getBody()).isEqualTo(deposited);
        assertThat(chequeController.collect(1L, collectRequest).getBody()).isEqualTo(collected);
        assertThat(chequeController.bounce(1L, bounceRequest).getBody()).isEqualTo(bounced);
        assertThat(chequeController.endorse(1L, endorseRequest).getBody()).isEqualTo(endorsed);
        assertThat(chequeController.cancel(1L, "iptal").getBody()).isEqualTo(cancelled);
    }

    @Test
    void summaryAndUpcomingEndpoints_delegateToService() {
        ChequePortfolioSummaryDto summary = new ChequePortfolioSummaryDto(
                1, new BigDecimal("100.00"),
                1, new BigDecimal("100.00"),
                0, BigDecimal.ZERO,
                0, BigDecimal.ZERO,
                0, BigDecimal.ZERO,
                1, new BigDecimal("100.00"),
                0, BigDecimal.ZERO,
                0, BigDecimal.ZERO);
        ChequeResponseDto upcoming = responseDto(1L, ChequeStatus.IN_PORTFOLIO);
        when(chequeService.getPortfolioSummary()).thenReturn(summary);
        when(chequeService.getUpcomingCheques(7)).thenReturn(List.of(upcoming));

        assertThat(chequeController.getPortfolioSummary().getBody()).isEqualTo(summary);
        assertThat(chequeController.getUpcoming(7).getBody()).containsExactly(upcoming);
    }

    private ChequeRequestDto requestDto() {
        return new ChequeRequestDto(
                "CHK-001",
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                20L,
                "Test Bankasi",
                "Merkez",
                "123456",
                new BigDecimal("100.00"),
                Currency.TRY,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "Not");
    }

    private ChequeResponseDto responseDto(Long id, ChequeStatus status) {
        return new ChequeResponseDto(
                id,
                "CHK-00" + id,
                ChequeType.RECEIVABLE,
                ChequeKind.CHEQUE,
                20L,
                "ABC Musteri",
                "Test Bankasi",
                "Merkez",
                "123456",
                new BigDecimal("100.00"),
                Currency.TRY,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                status,
                null,
                null,
                null,
                null,
                "Not",
                List.of());
    }
}
