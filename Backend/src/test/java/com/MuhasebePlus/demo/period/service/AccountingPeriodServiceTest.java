package com.MuhasebePlus.demo.period.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.period.dto.request.ClosePeriodRequestDto;
import com.MuhasebePlus.demo.period.dto.request.ReopenPeriodRequestDto;
import com.MuhasebePlus.demo.period.dto.response.AccountingPeriodResponseDto;
import com.MuhasebePlus.demo.period.dto.response.YearEndSummaryResponseDto;
import com.MuhasebePlus.demo.period.entity.AccountingPeriod;
import com.MuhasebePlus.demo.period.entity.PeriodStatus;
import com.MuhasebePlus.demo.period.repository.AccountingPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountingPeriodServiceTest {

    @Mock private AccountingPeriodRepository periodRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;
    @Mock private SystemLogService systemLogService;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private AccountingPeriodService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 9L;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(periodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // Closes an existing open accounting period and records who closed it.
    @Test
    void closePeriod_whenOpenPeriodExists_marksClosedAndLogs() {
        AccountingPeriod period = period(2026, 5, PeriodStatus.OPEN);
        period.setPeriodId(15L);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(period));

        AccountingPeriodResponseDto result = service.closePeriod(new ClosePeriodRequestDto(2026, 5));

        assertThat(result.status()).isEqualTo(PeriodStatus.CLOSED);
        assertThat(result.closedBy()).isEqualTo(USER_ID);
        assertThat(period.getClosedAt()).isNotNull();
        verify(periodRepository).save(period);
        verify(systemLogService).log(eq(LogLevel.WARNING), org.mockito.ArgumentMatchers.contains("2026/05"));
    }

    // Creates a virtual missing period before closing it.
    @Test
    void closePeriod_whenPeriodMissing_createsThenClosesPeriod() {
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 6))
                .thenReturn(Optional.empty());

        AccountingPeriodResponseDto result = service.closePeriod(new ClosePeriodRequestDto(2026, 6));

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(6);
        assertThat(result.status()).isEqualTo(PeriodStatus.CLOSED);
        verify(companyRepository).getReferenceById(COMPANY_ID);
        verify(periodRepository, org.mockito.Mockito.times(2)).save(any(AccountingPeriod.class));
    }

    // Rejects closing a period that is already closed.
    @Test
    void closePeriod_whenAlreadyClosed_throwsBusinessException() {
        AccountingPeriod period = period(2026, 5, PeriodStatus.CLOSED);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.closePeriod(new ClosePeriodRequestDto(2026, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2026/05");
    }

    // Reopens a closed period and stores the audit reason.
    @Test
    void reopenPeriod_whenClosed_updatesStatusAndReason() {
        AccountingPeriod period = period(2026, 4, PeriodStatus.CLOSED);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 4))
                .thenReturn(Optional.of(period));

        AccountingPeriodResponseDto result =
                service.reopenPeriod(new ReopenPeriodRequestDto(2026, 4, "Duzeltme kaydi"));

        assertThat(result.status()).isEqualTo(PeriodStatus.OPEN);
        assertThat(result.reopenedBy()).isEqualTo(USER_ID);
        assertThat(result.reopenReason()).isEqualTo("Duzeltme kaydi");
        assertThat(period.getReopenedAt()).isNotNull();
        verify(systemLogService).log(eq(LogLevel.WARNING), org.mockito.ArgumentMatchers.contains("Duzeltme kaydi"));
    }

    // Rejects reopening a period that is already open.
    @Test
    void reopenPeriod_whenAlreadyOpen_throwsBusinessException() {
        AccountingPeriod period = period(2026, 4, PeriodStatus.OPEN);
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 4))
                .thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.reopenPeriod(new ReopenPeriodRequestDto(2026, 4, "Tekrar ac")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2026/04");

        verify(periodRepository, never()).save(period);
    }

    // Lists periods by mapping repository rows to response DTOs.
    @Test
    void listPeriods_mapsRepositoryPeriods() {
        when(periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(List.of(period(2026, 1, PeriodStatus.OPEN), period(2026, 2, PeriodStatus.CLOSED)));

        List<AccountingPeriodResponseDto> result = service.listPeriods(2026);

        assertThat(result).extracting(AccountingPeriodResponseDto::month).containsExactly(1, 2);
        assertThat(result.get(1).status()).isEqualTo(PeriodStatus.CLOSED);
    }

    // Returns an open virtual status when a persisted period does not exist yet.
    @Test
    void getPeriodStatus_whenPeriodMissing_returnsVirtualOpenPeriod() {
        when(periodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 7))
                .thenReturn(Optional.empty());

        AccountingPeriodResponseDto result = service.getPeriodStatus(2026, 7);

        assertThat(result.periodId()).isNull();
        assertThat(result.status()).isEqualTo(PeriodStatus.OPEN);
        assertThat(result.month()).isEqualTo(7);
    }

    // Rejects year-end close until all 12 periods exist.
    @Test
    void closeYear_whenLessThanTwelvePeriodsExist_throwsBusinessException() {
        when(periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(List.of(period(2026, 1, PeriodStatus.CLOSED)));

        assertThatThrownBy(() -> service.closeYear(2026))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12");
    }

    // Rejects year-end close while any month is still open.
    @Test
    void closeYear_whenAnyPeriodOpen_throwsBusinessException() {
        List<AccountingPeriod> periods = closedYear(2026);
        periods.get(0).setStatus(PeriodStatus.OPEN);
        when(periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(periods);

        assertThatThrownBy(() -> service.closeYear(2026))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kapal");
    }

    // Rejects duplicate year-end close when December already has a close timestamp.
    @Test
    void closeYear_whenAlreadyYearEndClosed_throwsBusinessException() {
        List<AccountingPeriod> periods = closedYear(2026);
        periods.get(11).setYearEndClosedAt(LocalDateTime.of(2026, 12, 31, 23, 0));
        when(periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(periods);

        assertThatThrownBy(() -> service.closeYear(2026))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten");
    }

    // Creates year-end closing entry and stores the resulting journal entry id on December.
    @Test
    void closeYear_whenAllPeriodsClosed_recordsClosingEntryOnDecember() {
        List<AccountingPeriod> periods = closedYear(2026);
        AccountingPeriod december = periods.get(11);
        when(periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(COMPANY_ID, 2026))
                .thenReturn(periods);
        when(journalEntryService.createYearEndClosingEntry(COMPANY_ID, 2026)).thenReturn(77L);

        YearEndSummaryResponseDto result = service.closeYear(2026);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.closingJournalEntryId()).isEqualTo(77L);
        assertThat(december.getYearEndClosedBy()).isEqualTo(USER_ID);
        assertThat(december.getClosingJournalEntryId()).isEqualTo(77L);
        verify(periodRepository).save(december);
    }

    // Initializes missing periods for the previous, current, and next year.
    @Test
    void initializePeriodsForCompany_whenAllMissing_savesThirtySixOpenPeriods() {
        when(periodRepository.existsByCompanyCompanyIdAndYearAndMonth(eq(COMPANY_ID), any(Integer.class), any(Integer.class)))
                .thenReturn(false);
        ArgumentCaptor<List<AccountingPeriod>> captor = ArgumentCaptor.captor();

        service.initializePeriodsForCompany(COMPANY_ID);

        verify(periodRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(36);
        assertThat(captor.getValue()).allMatch(p -> p.getStatus() == PeriodStatus.OPEN);
        assertThat(captor.getValue()).allMatch(p -> p.getCompany() == company);
    }

    private AccountingPeriod period(int year, int month, PeriodStatus status) {
        return AccountingPeriod.builder()
                .company(company)
                .year(year)
                .month(month)
                .status(status)
                .build();
    }

    private List<AccountingPeriod> closedYear(int year) {
        List<AccountingPeriod> periods = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            periods.add(period(year, month, PeriodStatus.CLOSED));
        }
        return periods;
    }
}
