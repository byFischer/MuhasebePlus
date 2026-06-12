package com.MuhasebePlus.demo.company.controller;

import com.MuhasebePlus.demo.company.dto.request.CompanyRequestDto;
import com.MuhasebePlus.demo.company.dto.response.CompanyResponseDto;
import com.MuhasebePlus.demo.company.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyControllerUnitTest {

    @Mock private CompanyService companyService;

    @InjectMocks private CompanyController companyController;

    @Test
    void companyByIdEndpoints_delegateToService() {
        CompanyRequestDto request = requestDto();
        CompanyResponseDto response = responseDto(1L, "Test A.S.");
        when(companyService.getCompanyById(1L)).thenReturn(response);
        when(companyService.updateCompany(1L, request)).thenReturn(response);

        var getResponse = companyController.getCompanyById(1L);
        var updateResponse = companyController.updateCompany(1L, request);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(response);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isEqualTo(response);
    }

    @Test
    void currentCompanyEndpoints_delegateToService() {
        CompanyRequestDto request = requestDto();
        CompanyResponseDto response = responseDto(1L, "Current A.S.");
        when(companyService.getCurrentCompany()).thenReturn(response);
        when(companyService.updateCurrentCompany(request)).thenReturn(response);

        var getResponse = companyController.getCurrentCompany();
        var updateResponse = companyController.updateCurrentCompany(request);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(response);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isEqualTo(response);
    }

    private CompanyRequestDto requestDto() {
        return new CompanyRequestDto(
                "Test A.S.",
                "Kadikoy",
                "1111111111",
                "Adres",
                "Istanbul",
                "555",
                "info@test.com",
                "034",
                "TR-1",
                "1234567890123456",
                "6201",
                "MONTHLY",
                "GELIR_VERGISI");
    }

    private CompanyResponseDto responseDto(Long id, String name) {
        return new CompanyResponseDto(
                id,
                name,
                "Kadikoy",
                "1111111111",
                "Adres",
                "Istanbul",
                "555",
                "info@test.com",
                true,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 2, 10, 0),
                "034",
                "TR-1",
                "1234567890123456",
                "6201",
                "MONTHLY",
                "GELIR_VERGISI");
    }
}
