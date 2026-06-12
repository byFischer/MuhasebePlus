package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclaration;
import com.MuhasebePlus.demo.declaration.entity.WithholdingDeclarationLine;
import com.MuhasebePlus.demo.declaration.repository.WithholdingDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import com.MuhasebePlus.demo.tax.repository.WithholdingTaxCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class WithholdingDeclarationService {

    private final WithholdingDeclarationRepository repository;
    private final InvoiceVatSummaryRepository vatSummaryRepository;
    private final WithholdingTaxCodeRepository taxCodeRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    public WithholdingDeclaration generate(int year, int month) {
        Long companyId = companyContext.getCurrentCompanyId();

        repository.findByCompanyCompanyIdAndYearAndMonthAndIsDeletedFalse(companyId, year, month)
                .ifPresent(existing -> {
                    if (existing.getStatus() == DeclarationStatus.FINALIZED)
                        throw new BusinessException("Bu dönem Muhtasar beyannamesi zaten onaylanmış");
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    // Eski kaydın soft-delete'i yeni INSERT'ten önce DB'ye yazılsın
                    repository.saveAndFlush(existing);
                });

        List<InvoiceVatSummary> withholdingSummaries =
                vatSummaryRepository.findWithholdingsForPeriod(companyId, year, month);

        // Tevkifat kodu bazında grupla
        Map<Integer, BigDecimal> baseByCode  = new LinkedHashMap<>();
        Map<Integer, BigDecimal> amtByCode   = new LinkedHashMap<>();
        Map<Integer, Integer>    countByCode = new LinkedHashMap<>();

        for (InvoiceVatSummary s : withholdingSummaries) {
            Integer codeId = s.getWithholdingTaxCodeId();
            baseByCode.merge(codeId,
                    s.getWithholdingBaseAmount() != null ? s.getWithholdingBaseAmount() : BigDecimal.ZERO,
                    BigDecimal::add);
            amtByCode.merge(codeId,
                    s.getWithholdingAmount() != null ? s.getWithholdingAmount() : BigDecimal.ZERO,
                    BigDecimal::add);
            countByCode.merge(codeId, 1, (a, b) -> a + b);
        }

        // Tevkifat kodlarını toplu yükle
        Map<Integer, WithholdingTaxCode> codeMap = new HashMap<>();
        if (!baseByCode.isEmpty()) {
            taxCodeRepository.findAllById(baseByCode.keySet())
                    .forEach(c -> codeMap.put(c.getCodeId(), c));
        }

        List<WithholdingDeclarationLine> lines = new ArrayList<>();
        int order = 0;
        for (Map.Entry<Integer, BigDecimal> entry : baseByCode.entrySet()) {
            Integer codeId = entry.getKey();
            WithholdingTaxCode taxCode = codeMap.get(codeId);
            lines.add(WithholdingDeclarationLine.builder()
                    .withholdingTaxCodeId(codeId)
                    .withholdingCode(taxCode != null ? taxCode.getCode() : String.valueOf(codeId))
                    .description(taxCode != null ? taxCode.getDescription() : "Tevkifat kodu: " + codeId)
                    .baseAmount(entry.getValue())
                    .withholdingAmount(amtByCode.getOrDefault(codeId, BigDecimal.ZERO))
                    .documentCount(countByCode.getOrDefault(codeId, 0))
                    .lineOrder(order++)
                    .build());
        }

        BigDecimal totalBase = lines.stream()
                .map(WithholdingDeclarationLine::getBaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = lines.stream()
                .map(WithholdingDeclarationLine::getWithholdingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WithholdingDeclaration declaration = WithholdingDeclaration.builder()
                .company(companyRepository.getReferenceById(companyId))
                .year(year)
                .month(month)
                .status(DeclarationStatus.DRAFT)
                .totalWithholdingBase(totalBase)
                .totalWithholdingAmount(totalAmount)
                .build();

        lines.forEach(l -> l.setDeclaration(declaration));
        declaration.setLines(lines);

        return repository.save(declaration);
    }

    public WithholdingDeclaration finalize(Long declarationId) {
        WithholdingDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT formlar onaylanabilir");
        decl.setStatus(DeclarationStatus.FINALIZED);
        return repository.save(decl);
    }

    public List<WithholdingDeclaration> list() {
        return repository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(
                companyContext.getCurrentCompanyId());
    }

    public WithholdingDeclaration getById(Long id) {
        return findByIdAndCompany(id);
    }

    public void delete(Long id) {
        WithholdingDeclaration decl = findByIdAndCompany(id);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT formlar silinebilir");
        decl.setDeleted(true);
        decl.setDeletedAt(LocalDateTime.now());
        repository.save(decl);
    }

    private WithholdingDeclaration findByIdAndCompany(Long id) {
        return repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(
                        id, companyContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessException("Muhtasar beyannamesi bulunamadı: " + id));
    }
}
