package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AiQuotaGuardService {

    @Autowired private AiQuotaRepository quotaRepository;
    @Autowired private CompanyRepository companyRepository;

    @Value("${app.ai.monthly-token-budget:200000}")
    private int defaultMonthlyBudget;

    @Transactional(readOnly = true)
    public void check(Long companyId) {
        AiQuota quota = getOrCreate(companyId);
        resetIfExpired(quota);

        if (quota.getTokensUsedThisMonth() >= quota.getMonthlyTokenBudget()) {
            throw new RuntimeException("Aylık AI token kotanız doldu. Kota: " + quota.getMonthlyTokenBudget());
        }
    }

    @Transactional
    public void recordUsage(Long companyId, int inputTokens, int outputTokens) {
        AiQuota quota = getOrCreate(companyId);
        resetIfExpired(quota);
        quota.setTokensUsedThisMonth(quota.getTokensUsedThisMonth() + inputTokens + outputTokens);
        quotaRepository.save(quota);
    }

    @Transactional(readOnly = true)
    public AiQuota getQuota(Long companyId) {
        AiQuota quota = getOrCreate(companyId);
        resetIfExpired(quota);
        return quota;
    }

    private AiQuota getOrCreate(Long companyId) {
        return quotaRepository.findByCompanyCompanyId(companyId).orElseGet(() -> {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));
            AiQuota q = new AiQuota();
            q.setCompany(company);
            q.setMonthlyTokenBudget(defaultMonthlyBudget);
            q.setTokensUsedThisMonth(0);
            return quotaRepository.save(q);
        });
    }

    private void resetIfExpired(AiQuota quota) {
        if (quota.getResetAt() != null && LocalDateTime.now().isAfter(quota.getResetAt())) {
            quota.setTokensUsedThisMonth(0);
            quota.setResetAt(LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0));
            quotaRepository.save(quota);
        }
    }
}
