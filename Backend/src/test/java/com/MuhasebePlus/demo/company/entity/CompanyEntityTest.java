package com.MuhasebePlus.demo.company.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyEntityTest {

    @Test
    void companyAllArgsConstructorAndAccessorsWork() {
        Company company = new Company(
                1L,
                "Test A.S.",
                "Kadikoy",
                "1111111111",
                "Adres",
                "Istanbul",
                "555",
                "info@test.com",
                true,
                "034",
                "TR-1",
                "1234567890123456",
                "6201",
                "MONTHLY",
                "GELIR_VERGISI");

        assertThat(company.getCompanyId()).isEqualTo(1L);
        assertThat(company.getCompanyName()).isEqualTo("Test A.S.");
        assertThat(company.getTaxOffice()).isEqualTo("Kadikoy");
        assertThat(company.getTaxNumber()).isEqualTo("1111111111");
        assertThat(company.getAddress()).isEqualTo("Adres");
        assertThat(company.getCity()).isEqualTo("Istanbul");
        assertThat(company.getPhone()).isEqualTo("555");
        assertThat(company.getEmail()).isEqualTo("info@test.com");
        assertThat(company.isActive()).isTrue();
        assertThat(company.getTaxOfficeCode()).isEqualTo("034");
        assertThat(company.getTradeRegistryNo()).isEqualTo("TR-1");
        assertThat(company.getMersisNo()).isEqualTo("1234567890123456");
        assertThat(company.getNace()).isEqualTo("6201");
        assertThat(company.getDeclarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(company.getCorporateTaxType()).isEqualTo("GELIR_VERGISI");
    }

    @Test
    void builderAppliesDomainDefaultsAndSettersWork() {
        Company company = Company.builder()
                .companyId(2L)
                .companyName("Default A.S.")
                .taxNumber("2222222222")
                .build();

        assertThat(company.isActive()).isTrue();
        assertThat(company.getDeclarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(company.getCorporateTaxType()).isEqualTo("GELIR_VERGISI");

        company.setActive(false);
        company.setTaxOffice("Besiktas");
        company.setCity("Ankara");
        company.setDeclarationPeriodType("QUARTERLY");
        company.setCorporateTaxType("KURUMLAR_VERGISI");

        assertThat(company.isActive()).isFalse();
        assertThat(company.getTaxOffice()).isEqualTo("Besiktas");
        assertThat(company.getCity()).isEqualTo("Ankara");
        assertThat(company.getDeclarationPeriodType()).isEqualTo("QUARTERLY");
        assertThat(company.getCorporateTaxType()).isEqualTo("KURUMLAR_VERGISI");
    }

    @Test
    void noArgsConstructorStartsWithJavaDefaults() {
        Company company = new Company();

        assertThat(company.getCompanyId()).isNull();
        assertThat(company.getCompanyName()).isNull();
        assertThat(company.isActive()).isTrue();
        assertThat(company.getDeclarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(company.getCorporateTaxType()).isEqualTo("GELIR_VERGISI");
    }
}
