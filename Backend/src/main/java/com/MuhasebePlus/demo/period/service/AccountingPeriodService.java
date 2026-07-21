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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountingPeriodService {

    private final AccountingPeriodRepository periodRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;
    private final SystemLogService systemLogService;
    private final JournalEntryService journalEntryService;

    public AccountingPeriodResponseDto closePeriod(ClosePeriodRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        AccountingPeriod period = findOrCreateOpen(companyId, dto.year(), dto.month());

        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new BusinessException(
                    String.format("%d/%02d dönemi zaten kapalı.", dto.year(), dto.month()));
        }

        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(userId);
        AccountingPeriod saved = periodRepository.save(period);

        systemLogService.log(LogLevel.WARNING,
                String.format("Muhasebe dönemi kapatıldı: %d/%02d (kullanıcı: %d)", dto.year(), dto.month(), userId));

        return toResponseDto(saved);
    }

    public AccountingPeriodResponseDto reopenPeriod(ReopenPeriodRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        AccountingPeriod period = periodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, dto.year(), dto.month())
                .orElseThrow(() -> new BusinessException(
                        String.format("%d/%02d dönemi bulunamadı.", dto.year(), dto.month())));

        if (period.getStatus() == PeriodStatus.OPEN) {
            throw new BusinessException(
                    String.format("%d/%02d dönemi zaten açık.", dto.year(), dto.month()));
        }

        period.setStatus(PeriodStatus.OPEN);
        period.setReopenedAt(LocalDateTime.now());
        period.setReopenedBy(userId);
        period.setReopenReason(dto.reason());
        AccountingPeriod saved = periodRepository.save(period);

        systemLogService.log(LogLevel.WARNING,
                String.format("Muhasebe dönemi yeniden açıldı: %d/%02d — Sebep: %s (kullanıcı: %d)",
                        dto.year(), dto.month(), dto.reason(), userId));

        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountingPeriodResponseDto> listPeriods(int year) {
        Long companyId = companyContext.getCurrentCompanyId();
        return periodRepository
                .findByCompanyCompanyIdAndYearOrderByMonthAsc(companyId, year)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountingPeriodResponseDto getPeriodStatus(int year, int month) {
        Long companyId = companyContext.getCurrentCompanyId();
        AccountingPeriod period = periodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, year, month)
                .orElse(buildVirtualOpen(companyId, year, month));
        return toResponseDto(period);
    }

    public YearEndSummaryResponseDto closeYear(int year) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        List<AccountingPeriod> yearPeriods =
                periodRepository.findByCompanyCompanyIdAndYearOrderByMonthAsc(companyId, year);

        if (yearPeriods.size() < 12) {
            throw new BusinessException(year + " yılının tüm 12 dönemi bulunamadı.");
        }

        boolean allClosed = yearPeriods.stream().allMatch(p -> p.getStatus() == PeriodStatus.CLOSED);
        if (!allClosed) {
            throw new BusinessException(year + " yılının tüm dönemleri kapalı değil. Önce tüm ayları kapatın.");
        }

        AccountingPeriod dec = yearPeriods.stream()
                .filter(p -> p.getMonth() == 12)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Aralık dönemi bulunamadı."));

        if (dec.getYearEndClosedAt() != null) {
            throw new BusinessException(year + " yılı sonu zaten kapatılmış.");
        }

        Long closingEntryId = journalEntryService.createYearEndClosingEntry(companyId, year);

        LocalDateTime now = LocalDateTime.now();
        dec.setYearEndClosedAt(now);
        dec.setYearEndClosedBy(userId);
        if (closingEntryId != null) {
            dec.setClosingJournalEntryId(closingEntryId);
        }
        periodRepository.save(dec);

        systemLogService.log(LogLevel.WARNING,
                String.format("%d yılı sonu kapanışı gerçekleştirildi (kullanıcı: %d, kapanış fişi: %s)",
                        year, userId, closingEntryId));

        return new YearEndSummaryResponseDto(
                year,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                closingEntryId,
                now,
                year + " yılı başarıyla kapatıldı."
        );
    }

    /**
     * Yeni şirket oluşturulduğunda çağrılır.
     * Geçen yıl + bu yıl + gelecek yıl için tüm 36 ayı OPEN olarak oluşturur.
     */
    public void initializePeriodsForCompany(Long companyId) {
        Company company = companyRepository.getReferenceById(companyId);
        int currentYear = LocalDateTime.now().getYear();

        List<AccountingPeriod> periods = new ArrayList<>();
        for (int y = currentYear - 1; y <= currentYear + 1; y++) {
            for (int m = 1; m <= 12; m++) {
                if (!periodRepository.existsByCompanyCompanyIdAndYearAndMonth(companyId, y, m)) {
                    periods.add(AccountingPeriod.builder()
                            .company(company)
                            .year(y)
                            .month(m)
                            .status(PeriodStatus.OPEN)
                            .build());
                }
            }
        }
        periodRepository.saveAll(periods);
    }

    // --- PRIVATE ---

    private AccountingPeriod findOrCreateOpen(Long companyId, int year, int month) {
        return periodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, year, month)
                .orElseGet(() -> {
                    AccountingPeriod p = AccountingPeriod.builder()
                            .company(companyRepository.getReferenceById(companyId))
                            .year(year)
                            .month(month)
                            .status(PeriodStatus.OPEN)
                            .build();
                    return periodRepository.save(p);
                });
    }

    private AccountingPeriod buildVirtualOpen(Long companyId, int year, int month) {
        AccountingPeriod virtual = new AccountingPeriod();
        virtual.setYear(year);
        virtual.setMonth(month);
        virtual.setStatus(PeriodStatus.OPEN);
        return virtual;
    }

    private AccountingPeriodResponseDto toResponseDto(AccountingPeriod p) {
        return new AccountingPeriodResponseDto(
                p.getPeriodId(),
                p.getYear(),
                p.getMonth(),
                p.getStatus(),
                p.getClosedAt(),
                p.getClosedBy(),
                p.getReopenedAt(),
                p.getReopenedBy(),
                p.getReopenReason()
        );
    }
}
