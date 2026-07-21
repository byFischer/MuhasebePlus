package com.MuhasebePlus.demo.period.service;

import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.period.entity.AccountingPeriod;
import com.MuhasebePlus.demo.period.entity.PeriodStatus;
import com.MuhasebePlus.demo.period.repository.AccountingPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodGuardTest {

    @Mock
    private AccountingPeriodRepository periodRepository;

    @Mock
    private CompanyContext companyContext;

    @InjectMocks
    private AccountingPeriodGuard guard;

    private static final Long COMPANY_ID = 1L;

    @Test
    void assertOpen_whenDateIsNull_doesNothing() {
        assertThatNoException().isThrownBy(() -> guard.assertOpen(null));
        verifyNoInteractions(periodRepository, companyContext);
    }

    @Test
    void assertOpen_whenNoPeriodRecord_passesThrough() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 5, 15)));
    }

    @Test
    void assertOpen_whenPeriodIsOpen_passesThrough() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        AccountingPeriod period = AccountingPeriod.builder()
                .year(2026).month(5).status(PeriodStatus.OPEN).build();
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(period));

        assertThatNoException().isThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 5, 15)));
    }

    @Test
    void assertOpen_whenPeriodIsClosed_throwsClosedPeriodException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        AccountingPeriod period = AccountingPeriod.builder()
                .year(2026).month(3).status(PeriodStatus.CLOSED).build();
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 3))
                .thenReturn(Optional.of(period));

        assertThatThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 3, 1)))
                .isInstanceOf(ClosedPeriodException.class)
                .hasMessageContaining("2026")
                .hasMessageContaining("03");
    }

    @Test
    void assertOpen_usesCurrentCompanyId_notHardcoded() {
        Long otherCompanyId = 99L;
        when(companyContext.getCurrentCompanyId()).thenReturn(otherCompanyId);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(otherCompanyId, 2026, 1))
                .thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 1, 10)));
        verify(periodRepository).findByCompanyCompanyIdAndYearAndMonth(eq(otherCompanyId), anyInt(), anyInt());
        verify(periodRepository, never()).findByCompanyCompanyIdAndYearAndMonth(eq(COMPANY_ID), anyInt(), anyInt());
    }

    @Test
    void assertOpen_whenPeriodClosedForDifferentMonth_doesNotThrow() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        // March is closed, but we're querying April
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 4))
                .thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> guard.assertOpen(LocalDate.of(2026, 4, 1)));
    }
}
