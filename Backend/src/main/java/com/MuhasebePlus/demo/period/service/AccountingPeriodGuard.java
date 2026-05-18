package com.MuhasebePlus.demo.period.service;

import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.period.entity.AccountingPeriod;
import com.MuhasebePlus.demo.period.entity.PeriodStatus;
import com.MuhasebePlus.demo.period.repository.AccountingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountingPeriodGuard {

    private final AccountingPeriodRepository periodRepository;
    private final CompanyContext companyContext;

    /**
     * Verilen tarihin ait olduğu muhasebe döneminin açık olup olmadığını kontrol eder.
     * Dönem kapalıysa ClosedPeriodException fırlatır.
     * Dönem kaydı yoksa (OPEN varsayımı) geçirir.
     */
    public void assertOpen(LocalDate date) {
        if (date == null) return;

        Long companyId = companyContext.getCurrentCompanyId();
        int year = date.getYear();
        int month = date.getMonthValue();

        Optional<AccountingPeriod> period = periodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, year, month);

        if (period.isPresent() && period.get().getStatus() == PeriodStatus.CLOSED) {
            throw new ClosedPeriodException(year, month);
        }
    }
}
