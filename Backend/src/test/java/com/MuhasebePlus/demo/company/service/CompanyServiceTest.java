package com.MuhasebePlus.demo.company.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.dto.request.CompanyRequestDto;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks private CompanyService companyService;

    @Test
    void getCompanyById_whenCurrentCompanyMatches_returnsMappedResponse() {
        Company company = company();
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        var response = companyService.getCompanyById(1L);

        assertThat(response.companyId()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Test A.S.");
        assertThat(response.taxOffice()).isEqualTo("Kadikoy");
        assertThat(response.taxNumber()).isEqualTo("1111111111");
        assertThat(response.address()).isEqualTo("Adres");
        assertThat(response.city()).isEqualTo("Istanbul");
        assertThat(response.phone()).isEqualTo("555");
        assertThat(response.email()).isEqualTo("info@test.com");
        assertThat(response.isActive()).isTrue();
        assertThat(response.taxOfficeCode()).isEqualTo("034");
        assertThat(response.tradeRegistryNo()).isEqualTo("TR-1");
        assertThat(response.mersisNo()).isEqualTo("1234567890123456");
        assertThat(response.nace()).isEqualTo("6201");
        assertThat(response.declarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(response.corporateTaxType()).isEqualTo("GELIR_VERGISI");
    }

    @Test
    void getCompanyById_whenTryingAnotherCompany_throwsBeforeRepositoryLookup() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);

        assertThatThrownBy(() -> companyService.getCompanyById(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz yok");

        verify(companyRepository, never()).findById(any());
    }

    @Test
    void getCompanyById_whenCompanyMissing_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found: 1");
    }

    @Test
    void updateCompany_whenAuthorizedAndTaxNumberUnchanged_updatesFieldsWithoutDuplicateCheck() {
        Company company = company();
        CompanyRequestDto request = request("1111111111", null, null);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = companyService.updateCompany(1L, request);

        assertThat(response.companyName()).isEqualTo("Yeni A.S.");
        assertThat(response.taxNumber()).isEqualTo("1111111111");
        assertThat(response.declarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(response.corporateTaxType()).isEqualTo("GELIR_VERGISI");
        verify(companyRepository, never()).existsByTaxNumber(any());
        verify(companyRepository).save(company);
    }

    @Test
    void updateCompany_whenCompanyMissing_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateCompany(1L, request("1111111111", null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found: 1");

        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateCompany_whenTaxNumberChangesAndExists_throwsBeforeSaving() {
        Company company = company();
        CompanyRequestDto request = request("2222222222", "QUARTERLY", "KURUMLAR_VERGISI");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByTaxNumber("2222222222")).thenReturn(true);

        assertThatThrownBy(() -> companyService.updateCompany(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tax number already exists: 2222222222");

        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateCompany_whenTaxNumberChangesAndIsUnique_updatesOptionalTaxSettings() {
        Company company = company();
        CompanyRequestDto request = request("2222222222", "QUARTERLY", "KURUMLAR_VERGISI");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByTaxNumber("2222222222")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = companyService.updateCompany(1L, request);

        assertThat(response.taxNumber()).isEqualTo("2222222222");
        assertThat(response.declarationPeriodType()).isEqualTo("QUARTERLY");
        assertThat(response.corporateTaxType()).isEqualTo("KURUMLAR_VERGISI");
    }

    @Test
    void updateCompany_whenTryingAnotherCompany_throwsBeforeRepositoryLookup() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);

        assertThatThrownBy(() -> companyService.updateCompany(2L, request("1111111111", null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz yok");

        verify(companyRepository, never()).findById(any());
    }

    @Test
    void getCurrentCompany_usesCompanyFromContext() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company()));

        var response = companyService.getCurrentCompany();

        assertThat(response.companyId()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Test A.S.");
    }

    @Test
    void getCurrentCompany_whenCompanyMissing_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCurrentCompany())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void updateCurrentCompany_whenTaxNumberChangesAndIsUnique_updatesCompany() {
        Company company = company();
        CompanyRequestDto request = request("3333333333", "MONTHLY", "GELIR_VERGISI");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByTaxNumber("3333333333")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = companyService.updateCurrentCompany(request);

        assertThat(response.companyName()).isEqualTo("Yeni A.S.");
        assertThat(response.taxNumber()).isEqualTo("3333333333");
        verify(companyRepository).save(company);
    }

    @Test
    void updateCurrentCompany_whenTaxNumberAndOptionalTaxSettingsAreNull_keepsExistingTaxSettings() {
        Company company = company();
        CompanyRequestDto request = request(null, null, null);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = companyService.updateCurrentCompany(request);

        assertThat(response.taxNumber()).isNull();
        assertThat(response.declarationPeriodType()).isEqualTo("MONTHLY");
        assertThat(response.corporateTaxType()).isEqualTo("GELIR_VERGISI");
        verify(companyRepository, never()).existsByTaxNumber(any());
    }

    @Test
    void updateCurrentCompany_whenDuplicateTaxNumber_throwsBeforeSaving() {
        Company company = company();
        CompanyRequestDto request = request("3333333333", null, null);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByTaxNumber("3333333333")).thenReturn(true);

        assertThatThrownBy(() -> companyService.updateCurrentCompany(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("3333333333");

        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateCurrentCompany_whenCompanyMissing_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateCurrentCompany(request("1111111111", null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found");

        verify(companyRepository, never()).save(any());
    }

    private Company company() {
        return Company.builder()
                .companyId(1L)
                .companyName("Test A.S.")
                .taxOffice("Kadikoy")
                .taxNumber("1111111111")
                .address("Adres")
                .city("Istanbul")
                .phone("555")
                .email("info@test.com")
                .isActive(true)
                .taxOfficeCode("034")
                .tradeRegistryNo("TR-1")
                .mersisNo("1234567890123456")
                .nace("6201")
                .declarationPeriodType("MONTHLY")
                .corporateTaxType("GELIR_VERGISI")
                .build();
    }

    private CompanyRequestDto request(String taxNumber, String declarationPeriodType, String corporateTaxType) {
        return new CompanyRequestDto(
                "Yeni A.S.",
                "Besiktas",
                taxNumber,
                "Yeni Adres",
                "Ankara",
                "444",
                "new@test.com",
                "006",
                "TR-2",
                "9876543210987654",
                "6920",
                declarationPeriodType,
                corporateTaxType);
    }
}
