package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.GenericDeclaration;
import com.MuhasebePlus.demo.declaration.repository.GenericDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class GenericDeclarationService {

    public static final String KDV2           = "KDV2";
    public static final String TEMPORARY_TAX  = "TEMPORARY_TAX";
    public static final String CORPORATE_TAX  = "CORPORATE_TAX";

    private final GenericDeclarationRepository repository;
    private final InvoiceVatSummaryRepository vatSummaryRepository;
    private final JournalEntryLineRepository journalLineRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    public GenericDeclaration generate(String type, int year, int period) {
        validateType(type);
        Long companyId = companyContext.getCurrentCompanyId();

        repository.findByCompanyCompanyIdAndDeclarationTypeAndYearAndPeriodAndIsDeletedFalse(
                        companyId, type, year, period)
                .ifPresent(existing -> {
                    if (existing.getStatus() == DeclarationStatus.FINALIZED)
                        throw new BusinessException("Bu dönem " + type + " beyannamesi zaten onaylanmış");
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    // Eski kaydın soft-delete'i yeni INSERT'ten önce DB'ye yazılsın
                    repository.saveAndFlush(existing);
                });

        Map<String, Object> snapshot;
        BigDecimal totalPayable;

        switch (type) {
            case KDV2 -> {
                snapshot = buildKdv2Snapshot(companyId, year, period);
                totalPayable = (BigDecimal) snapshot.getOrDefault("totalWithholding", BigDecimal.ZERO);
            }
            case TEMPORARY_TAX -> {
                snapshot = buildTempTaxSnapshot(companyId, year, period);
                totalPayable = (BigDecimal) snapshot.getOrDefault("taxPayable", BigDecimal.ZERO);
            }
            case CORPORATE_TAX -> {
                snapshot = buildCorporateTaxSnapshot(companyId, year);
                totalPayable = (BigDecimal) snapshot.getOrDefault("taxPayable", BigDecimal.ZERO);
            }
            default -> throw new BusinessException("Bilinmeyen beyanname tipi: " + type);
        }

        GenericDeclaration decl = GenericDeclaration.builder()
                .company(companyRepository.getReferenceById(companyId))
                .declarationType(type)
                .year(year)
                .period(period)
                .status(DeclarationStatus.DRAFT)
                .dataSnapshot(snapshot)
                .totalPayable(totalPayable)
                .build();

        return repository.save(decl);
    }

    public GenericDeclaration finalize(Long declarationId) {
        GenericDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT durumundaki beyannameler onaylanabilir");
        decl.setStatus(DeclarationStatus.FINALIZED);
        return repository.save(decl);
    }

    public GenericDeclaration markSubmitted(Long declarationId) {
        GenericDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.FINALIZED)
            throw new BusinessException("Yalnızca FINALIZED durumundaki beyannameler gönderilmiş olarak işaretlenebilir");
        decl.setStatus(DeclarationStatus.SUBMITTED);
        return repository.save(decl);
    }

    public List<GenericDeclaration> list(String type) {
        validateType(type);
        return repository.findByCompanyCompanyIdAndDeclarationTypeAndIsDeletedFalseOrderByYearDescPeriodDesc(
                companyContext.getCurrentCompanyId(), type);
    }

    public GenericDeclaration getById(Long id) {
        return findByIdAndCompany(id);
    }

    public void delete(Long id) {
        GenericDeclaration decl = findByIdAndCompany(id);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT durumundaki beyannameler silinebilir");
        decl.setDeleted(true);
        decl.setDeletedAt(LocalDateTime.now());
        repository.save(decl);
    }

    // ─── Snapshot builders ───────────────────────────────────────────────────

    private Map<String, Object> buildKdv2Snapshot(Long companyId, int year, int month) {
        List<InvoiceVatSummary> summaries =
                vatSummaryRepository.findPurchaseWithholdingsForPeriod(companyId, year, month);

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalWithholding = BigDecimal.ZERO;
        Map<String, Object> byCode = new LinkedHashMap<>();

        for (InvoiceVatSummary s : summaries) {
            BigDecimal base = s.getWithholdingBaseAmount() != null ? s.getWithholdingBaseAmount() : BigDecimal.ZERO;
            BigDecimal amt  = s.getWithholdingAmount()     != null ? s.getWithholdingAmount()     : BigDecimal.ZERO;
            totalBase        = totalBase.add(base);
            totalWithholding = totalWithholding.add(amt);

            String codeKey = "code_" + s.getWithholdingTaxCodeId();
            @SuppressWarnings("unchecked")
            Map<String, Object> codeEntry = (Map<String, Object>) byCode.computeIfAbsent(codeKey, k -> new LinkedHashMap<>());
            codeEntry.put("codeId",    s.getWithholdingTaxCodeId());
            codeEntry.put("baseAmount", ((BigDecimal) codeEntry.getOrDefault("baseAmount", BigDecimal.ZERO)).add(base));
            codeEntry.put("withholdingAmount", ((BigDecimal) codeEntry.getOrDefault("withholdingAmount", BigDecimal.ZERO)).add(amt));
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("type",             KDV2);
        snapshot.put("year",             year);
        snapshot.put("month",            month);
        snapshot.put("totalBase",        totalBase);
        snapshot.put("totalWithholding", totalWithholding);
        snapshot.put("lines",            byCode);
        return snapshot;
    }

    private Map<String, Object> buildTempTaxSnapshot(Long companyId, int year, int quarter) {
        if (quarter < 1 || quarter > 4) throw new BusinessException("Geçici vergi için dönem 1-4 arasında olmalı");
        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate   = LocalDate.of(year, quarter * 3, 1).withDayOfMonth(
                LocalDate.of(year, quarter * 3, 1).lengthOfMonth());

        return buildProfitSnapshot(companyId, year, quarter, startDate, endDate, 0.15);
    }

    private Map<String, Object> buildCorporateTaxSnapshot(Long companyId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate   = LocalDate.of(year, 12, 31);
        return buildProfitSnapshot(companyId, year, 0, startDate, endDate, 0.25);
    }

    private Map<String, Object> buildProfitSnapshot(Long companyId, int year, int period,
                                                     LocalDate startDate, LocalDate endDate,
                                                     double taxRate) {
        List<Object[]> rows = journalLineRepository.sumByAccountTypeForPeriod(
                companyId, startDate, endDate,
                List.of(AccountType.INCOME, AccountType.EXPENSE, AccountType.COST));

        Map<AccountType, BigDecimal[]> totals = new HashMap<>();
        for (Object[] row : rows) {
            AccountType type  = (AccountType) row[0];
            BigDecimal debit  = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal credit = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            totals.put(type, new BigDecimal[]{debit, credit});
        }

        // INCOME: credit > debit = net income (alacak normal bakiye)
        BigDecimal[] incomeTotals  = totals.getOrDefault(AccountType.INCOME,  new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal[] expenseTotals = totals.getOrDefault(AccountType.EXPENSE, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal[] costTotals    = totals.getOrDefault(AccountType.COST,    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

        BigDecimal totalIncome  = incomeTotals[1].subtract(incomeTotals[0]);
        BigDecimal totalExpense = expenseTotals[0].subtract(expenseTotals[1]);
        BigDecimal totalCost    = costTotals[0].subtract(costTotals[1]);
        BigDecimal netProfit    = totalIncome.subtract(totalExpense).subtract(totalCost);
        BigDecimal taxBase      = netProfit.max(BigDecimal.ZERO);
        BigDecimal taxPayable   = taxBase.multiply(BigDecimal.valueOf(taxRate))
                                        .setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("year",         year);
        snapshot.put("period",       period);
        snapshot.put("startDate",    startDate.toString());
        snapshot.put("endDate",      endDate.toString());
        snapshot.put("totalIncome",  totalIncome);
        snapshot.put("totalExpense", totalExpense);
        snapshot.put("totalCost",    totalCost);
        snapshot.put("netProfit",    netProfit);
        snapshot.put("taxRate",      taxRate);
        snapshot.put("taxBase",      taxBase);
        snapshot.put("taxPayable",   taxPayable);
        return snapshot;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private GenericDeclaration findByIdAndCompany(Long id) {
        return repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(
                        id, companyContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessException("Beyanname bulunamadı: " + id));
    }

    private void validateType(String type) {
        if (!KDV2.equals(type) && !TEMPORARY_TAX.equals(type) && !CORPORATE_TAX.equals(type))
            throw new BusinessException("Geçersiz beyanname tipi. KDV2, TEMPORARY_TAX veya CORPORATE_TAX olmalı.");
    }
}
