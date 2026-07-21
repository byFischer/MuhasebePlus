package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclaration;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclarationLine;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.repository.BaBsDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class BaBsDeclarationService {

    private static final BigDecimal THRESHOLD = new BigDecimal("5000");

    private final BaBsDeclarationRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    public BaBsDeclaration generate(int year, int month, String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        validateType(type);

        // Mevcut draft varsa sil
        repository.findByCompanyCompanyIdAndBabsTypeAndYearAndMonthAndIsDeletedFalse(companyId, type, year, month)
                .ifPresent(existing -> {
                    if (existing.getStatus() == DeclarationStatus.FINALIZED)
                        throw new BusinessException("Bu dönem " + type + " formu zaten onaylanmış");
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    repository.save(existing);
                });

        InvoiceType invoiceType = "BS".equals(type) ? InvoiceType.sale : InvoiceType.purchase;
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Invoice> invoices = invoiceRepository.findForPeriod(companyId, invoiceType, start, end);

        // Müşteri bazında topla
        Map<Long, BigDecimal> totalByCustomer = new LinkedHashMap<>();
        Map<Long, Integer> countByCustomer = new LinkedHashMap<>();
        Map<Long, Invoice> sampleByCustomer = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            if (inv.getCustomerId() == null) continue;
            Long cid = inv.getCustomerId();
            totalByCustomer.merge(cid, inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO, BigDecimal::add);
            countByCustomer.merge(cid, 1, (a, b) -> a + b);
            sampleByCustomer.putIfAbsent(cid, inv);
        }

        // 5.000 TL eşiğini geçenleri filtrele
        List<BaBsDeclarationLine> lines = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : totalByCustomer.entrySet()) {
            if (entry.getValue().compareTo(THRESHOLD) >= 0) {
                Long customerId = entry.getKey();
                lines.add(BaBsDeclarationLine.builder()
                        .taxNumber("")
                        .customerName("Müşteri #" + customerId)
                        .totalAmount(entry.getValue())
                        .documentCount(countByCustomer.get(customerId))
                        .build());
            }
        }

        BigDecimal total = lines.stream()
                .map(BaBsDeclarationLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BaBsDeclaration decl = BaBsDeclaration.builder()
                .company(companyRepository.getReferenceById(companyId))
                .babsType(type)
                .year(year)
                .month(month)
                .status(DeclarationStatus.DRAFT)
                .recordCount(lines.size())
                .totalAmount(total)
                .build();

        lines.forEach(l -> l.setDeclaration(decl));
        decl.setLines(lines);

        return repository.save(decl);
    }

    public BaBsDeclaration finalize(Long declarationId) {
        BaBsDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT formlar onaylanabilir");
        decl.setStatus(DeclarationStatus.FINALIZED);
        return repository.save(decl);
    }

    public List<BaBsDeclaration> list() {
        return repository.findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(
                companyContext.getCurrentCompanyId());
    }

    public BaBsDeclaration getById(Long id) {
        return findByIdAndCompany(id);
    }

    public void delete(Long id) {
        BaBsDeclaration decl = findByIdAndCompany(id);
        if (decl.getStatus() != DeclarationStatus.DRAFT)
            throw new BusinessException("Yalnızca DRAFT formlar silinebilir");
        decl.setDeleted(true);
        decl.setDeletedAt(LocalDateTime.now());
        repository.save(decl);
    }

    private BaBsDeclaration findByIdAndCompany(Long id) {
        return repository.findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(
                        id, companyContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessException("Ba-Bs formu bulunamadı: " + id));
    }

    private void validateType(String type) {
        if (!"BA".equals(type) && !"BS".equals(type))
            throw new BusinessException("Geçersiz form tipi. 'BA' veya 'BS' olmalı.");
    }
}
