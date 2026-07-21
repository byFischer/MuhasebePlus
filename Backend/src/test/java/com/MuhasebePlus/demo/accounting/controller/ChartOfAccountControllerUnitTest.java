package com.MuhasebePlus.demo.accounting.controller;

import com.MuhasebePlus.demo.accounting.dto.request.ChartOfAccountRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.ChartOfAccountResponseDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.ChartOfAccountService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartOfAccountControllerUnitTest {

    @Mock private ChartOfAccountService chartOfAccountService;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private ChartOfAccountController controller;

    @Test
    void seedTdhp_copiesTemplateForCurrentCompany() {
        when(companyContext.getCurrentCompanyId()).thenReturn(7L);

        ResponseEntity<Void> response = controller.seedTdhp();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chartOfAccountService).copyTdhpForCompany(7L);
    }

    @Test
    void getAll_returnsServiceAccounts() {
        ChartOfAccountResponseDto account = accountDto(1L, "100");
        when(chartOfAccountService.getAll()).thenReturn(List.of(account));

        ResponseEntity<List<ChartOfAccountResponseDto>> response = controller.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(account);
    }

    @Test
    void getById_returnsServiceAccount() {
        ChartOfAccountResponseDto account = accountDto(10L, "120");
        when(chartOfAccountService.getById(10L)).thenReturn(account);

        ResponseEntity<ChartOfAccountResponseDto> response = controller.getById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(account);
    }

    @Test
    void create_returnsCreatedAccount() {
        ChartOfAccountRequestDto request = request("120.00.001");
        ChartOfAccountResponseDto created = accountDto(11L, "120.00.001");
        when(chartOfAccountService.create(request)).thenReturn(created);

        ResponseEntity<ChartOfAccountResponseDto> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void update_returnsUpdatedAccount() {
        ChartOfAccountRequestDto request = request("120.00.002");
        ChartOfAccountResponseDto updated = accountDto(12L, "120.00.002");
        when(chartOfAccountService.update(12L, request)).thenReturn(updated);

        ResponseEntity<ChartOfAccountResponseDto> response = controller.update(12L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    void delete_returnsNoContent() {
        ResponseEntity<Void> response = controller.delete(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(chartOfAccountService).delete(12L);
    }

    private ChartOfAccountRequestDto request(String code) {
        return new ChartOfAccountRequestDto(code, "Test hesap", AccountType.ASSET, null, true, "Aciklama");
    }

    private ChartOfAccountResponseDto accountDto(Long id, String code) {
        return new ChartOfAccountResponseDto(
                id,
                code,
                "Test hesap",
                AccountType.ASSET,
                null,
                null,
                true,
                false,
                "Aciklama",
                LocalDateTime.of(2026, 1, 1, 10, 0));
    }
}
