package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaGuardServiceTest {

    @Mock private AiQuotaRepository quotaRepository;
    @Mock private CompanyRepository companyRepository;

    @Test
    void check_whenQuotaMissing_createsQuotaWithDefaultBudget() {
        Company company = Company.builder().companyId(1L).companyName("Test").build();
        when(quotaRepository.findByCompanyCompanyId(1L)).thenReturn(Optional.empty());
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(quotaRepository.save(any(AiQuota.class))).thenAnswer(inv -> inv.getArgument(0));
        AiQuotaGuardService service = new AiQuotaGuardService(quotaRepository, companyRepository, 500);

        service.check(1L);

        verify(quotaRepository).save(org.mockito.ArgumentMatchers.argThat(q ->
                q.getCompany() == company && q.getMonthlyTokenBudget() == 500 && q.getTokensUsedThisMonth() == 0));
    }

    @Test
    void check_whenBudgetExceeded_throwsRuntimeException() {
        AiQuota quota = quota(1L, 100, 100, LocalDateTime.now().plusDays(1));
        when(quotaRepository.findByCompanyCompanyId(1L)).thenReturn(Optional.of(quota));
        AiQuotaGuardService service = new AiQuotaGuardService(quotaRepository, companyRepository, 500);

        assertThatThrownBy(() -> service.check(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("token");
    }

    @Test
    void recordUsage_resetsExpiredQuotaBeforeAddingUsage() {
        AiQuota quota = quota(1L, 1000, 900, LocalDateTime.now().minusDays(1));
        when(quotaRepository.findByCompanyCompanyId(1L)).thenReturn(Optional.of(quota));
        AiQuotaGuardService service = new AiQuotaGuardService(quotaRepository, companyRepository, 1000);

        service.recordUsage(1L, 40, 60);

        assertThat(quota.getTokensUsedThisMonth()).isEqualTo(100);
        assertThat(quota.getResetAt()).isAfter(LocalDateTime.now());
        verify(quotaRepository).save(quota);
    }

    @Test
    void getQuota_whenCompanyMissing_throwsRuntimeException() {
        when(quotaRepository.findByCompanyCompanyId(9L)).thenReturn(Optional.empty());
        when(companyRepository.findById(9L)).thenReturn(Optional.empty());
        AiQuotaGuardService service = new AiQuotaGuardService(quotaRepository, companyRepository, 500);

        assertThatThrownBy(() -> service.getQuota(9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found: 9");
    }

    private AiQuota quota(Long companyId, int budget, int used, LocalDateTime resetAt) {
        Company company = Company.builder().companyId(companyId).companyName("Test").build();
        AiQuota quota = new AiQuota();
        quota.setCompany(company);
        quota.setCompanyId(companyId);
        quota.setMonthlyTokenBudget(budget);
        quota.setTokensUsedThisMonth(used);
        quota.setResetAt(resetAt);
        return quota;
    }
}
