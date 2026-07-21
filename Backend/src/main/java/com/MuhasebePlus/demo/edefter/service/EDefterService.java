package com.MuhasebePlus.demo.edefter.service;

import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.edefter.entity.EDefterRun;
import com.MuhasebePlus.demo.edefter.entity.EDefterStatus;
import com.MuhasebePlus.demo.edefter.repository.EDefterRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EDefterService {

    private final EDefterRunRepository runRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    public EDefterRun generate(int year, int month) {
        Long companyId = companyContext.getCurrentCompanyId();

        runRepository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(companyId, year, month)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Bu dönem için e-Defter zaten üretildi (Çalışma ID: " + existing.getRunId() +
                            "). Yeniden üretmek için /regenerate endpoint'ini kullanın.");
                });

        return doGenerate(companyId, year, month);
    }

    public EDefterRun regenerate(Long runId) {
        Long companyId = companyContext.getCurrentCompanyId();
        EDefterRun existing = findByIdAndCompany(runId);
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now());
        runRepository.save(existing);
        return doGenerate(companyId, existing.getYear(), existing.getMonth());
    }

    public List<EDefterRun> listByYear(int year) {
        return runRepository.findByCompanyCompanyIdAndYearAndIsDeletedFalseOrderByMonthAsc(
                companyContext.getCurrentCompanyId(), year);
    }

    public List<EDefterRun> list() {
        return runRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(
                companyContext.getCurrentCompanyId());
    }

    public EDefterRun getById(Long runId) {
        return findByIdAndCompany(runId);
    }

    public String getJournalXml(Long runId) {
        EDefterRun run = findByIdAndCompany(runId);
        if (run.getJournalXml() == null) throw new BusinessException("Yevmiye XML henüz mevcut değil");
        return run.getJournalXml();
    }

    public String getLedgerXml(Long runId) {
        EDefterRun run = findByIdAndCompany(runId);
        if (run.getLedgerXml() == null) throw new BusinessException("Kebir XML henüz mevcut değil");
        return run.getLedgerXml();
    }

    public void delete(Long runId) {
        EDefterRun run = findByIdAndCompany(runId);
        run.setDeleted(true);
        run.setDeletedAt(LocalDateTime.now());
        runRepository.save(run);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private EDefterRun doGenerate(Long companyId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());

        List<JournalEntry> entries =
                journalEntryRepository.findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        companyId, start, end);

        Company company = companyRepository.getReferenceById(companyId);

        String journalXml = EDefterXmlBuilder.buildJournal(company, year, month, entries);
        String ledgerXml  = EDefterXmlBuilder.buildLedger(company, year, month, entries);

        int totalLines = entries.stream().mapToInt(e -> e.getLines().size()).sum();

        EDefterRun run = EDefterRun.builder()
                .company(company)
                .year(year)
                .month(month)
                .journalXml(journalXml)
                .ledgerXml(ledgerXml)
                .journalEntryCount(entries.size())
                .journalLineCount(totalLines)
                .status(EDefterStatus.GENERATED)
                .generatedAt(LocalDateTime.now())
                .build();

        return runRepository.save(run);
    }

    private EDefterRun findByIdAndCompany(Long runId) {
        return runRepository.findByRunIdAndCompanyCompanyIdAndIsDeletedFalse(
                        runId, companyContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessException("e-Defter çalışması bulunamadı: " + runId));
    }
}
