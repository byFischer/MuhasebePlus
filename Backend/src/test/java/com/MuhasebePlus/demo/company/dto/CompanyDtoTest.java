package com.MuhasebePlus.demo.company.dto;

import com.MuhasebePlus.demo.company.dto.request.CompanyRequestDto;
import com.MuhasebePlus.demo.company.dto.response.CompanyResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyDtoTest {

    @Test
    void requestRecordExposesConstructorValues() {
        CompanyRequestDto request = new CompanyRequestDto(
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

        assertThat(request.companyName()).isEqualTo("Test A.S.");
        assertThat(request.taxOffice()).isEqualTo("Kadikoy");
        assertThat(request.taxNumber()).isEqualTo("1111111111");
        assertThat(request.address()).isEqualTo("Adres");
        assertThat(request.city()).isEqualTo("Istanbul");
        assertThat(request.phone()).isEqualTo("555");
        assertThat(request.email()).isEqualTo("info@test.com");
        assertThat(request.taxOfficeCode()).isEqualTo("034");
        assertThat(request.tradeRegistryNo()).isEqualTo("TR-1");
        assertThat(request.mersisNo()).isEqualTo("1234567890123456");
        assertThat(request.nace()).isEqualTo("6201");
        assertThat(request.declarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(request.corporateTaxType()).isEqualTo("GELIR_VERGISI");
    }

    @Test
    void responseRecordExposesConstructorValues() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 2, 10, 0);
        CompanyResponseDto response = new CompanyResponseDto(
                1L,
                "Test A.S.",
                "Kadikoy",
                "1111111111",
                "Adres",
                "Istanbul",
                "555",
                "info@test.com",
                true,
                createdAt,
                updatedAt,
                "034",
                "TR-1",
                "1234567890123456",
                "6201",
                "MONTHLY",
                "GELIR_VERGISI");

        assertThat(response.companyId()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Test A.S.");
        assertThat(response.taxOffice()).isEqualTo("Kadikoy");
        assertThat(response.taxNumber()).isEqualTo("1111111111");
        assertThat(response.address()).isEqualTo("Adres");
        assertThat(response.city()).isEqualTo("Istanbul");
        assertThat(response.phone()).isEqualTo("555");
        assertThat(response.email()).isEqualTo("info@test.com");
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.taxOfficeCode()).isEqualTo("034");
        assertThat(response.tradeRegistryNo()).isEqualTo("TR-1");
        assertThat(response.mersisNo()).isEqualTo("1234567890123456");
        assertThat(response.nace()).isEqualTo("6201");
        assertThat(response.declarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(response.corporateTaxType()).isEqualTo("GELIR_VERGISI");
    }
}
