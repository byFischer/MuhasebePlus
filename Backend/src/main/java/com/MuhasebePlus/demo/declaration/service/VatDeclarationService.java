package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.declaration.dto.GenerateDeclarationRequestDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationLineDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.DeclarationStatus;
import com.MuhasebePlus.demo.declaration.entity.VatDeclaration;
import com.MuhasebePlus.demo.declaration.entity.VatDeclarationLine;
import com.MuhasebePlus.demo.declaration.repository.VatDeclarationRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service("declarationVatDeclarationService")
@Transactional
@RequiredArgsConstructor
public class VatDeclarationService {

    private static final String KDV1 = "KDV1";

    private final VatDeclarationRepository declarationRepository;
    private final InvoiceVatSummaryRepository vatSummaryRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    public VatDeclarationResponseDto generate(GenerateDeclarationRequestDto req) {
        Long companyId = companyContext.getCurrentCompanyId();
        int year = req.year();
        int month = req.month();

        // Aynı dönem için draft varsa güncelle, yoksa yeni oluştur
        declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(companyId, KDV1, year, month)
                .ifPresent(existing -> {
                    if (existing.getStatus() == DeclarationStatus.FINALIZED) {
                        throw new BusinessException("Bu dönem için beyanname zaten onaylanmış (FINALIZED). Değiştirilemez.");
                    }
                    existing.setDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    declarationRepository.save(existing);
                });

        // Satış KDV kırılımları
        List<InvoiceVatSummary> salesSummaries = vatSummaryRepository.findSalesVatSummaryForPeriod(companyId, year, month);
        // Alış KDV kırılımları
        List<InvoiceVatSummary> purchaseSummaries = vatSummaryRepository.findPurchaseVatSummaryForPeriod(companyId, year, month);

        // Oran bazında topla
        Map<BigDecimal, BigDecimal> salesBaseByRate = groupBase(salesSummaries);
        Map<BigDecimal, BigDecimal> salesVatByRate  = groupVat(salesSummaries);

        BigDecimal totalSalesBase = sum(salesSummaries.stream().map(InvoiceVatSummary::getVatBaseAmount).collect(Collectors.toList()));
        BigDecimal totalSalesVat  = sum(salesSummaries.stream().map(InvoiceVatSummary::getVatAmount).collect(Collectors.toList()));
        BigDecimal totalPurchasesBase = sum(purchaseSummaries.stream().map(InvoiceVatSummary::getVatBaseAmount).collect(Collectors.toList()));
        BigDecimal totalPurchasesVat  = sum(purchaseSummaries.stream().map(InvoiceVatSummary::getVatAmount).collect(Collectors.toList()));

        BigDecimal carryover = req.previousPeriodCarryover() != null ? req.previousPeriodCarryover() : BigDecimal.ZERO;
        // Önceki dönemden otomatik devir
        if (carryover.compareTo(BigDecimal.ZERO) == 0) {
            carryover = getPreviousPeriodCarryover(companyId, year, month);
        }

        BigDecimal net = totalSalesVat.subtract(totalPurchasesVat).subtract(carryover);
        BigDecimal payableVat;
        BigDecimal nextPeriodCarryover;
        if (net.compareTo(BigDecimal.ZERO) > 0) {
            payableVat = net;
            nextPeriodCarryover = BigDecimal.ZERO;
        } else {
            payableVat = BigDecimal.ZERO;
            nextPeriodCarryover = net.abs();
        }

        // Form satırlarını oluştur
        List<VatDeclarationLine> lines = new ArrayList<>();
        int order = 0;

        // Tablo I — oran bazlı satır
        for (Map.Entry<BigDecimal, BigDecimal> entry : salesBaseByRate.entrySet()) {
            BigDecimal rate = entry.getKey();
            String lineCode = rateToLineCode(rate);
            lines.add(buildLine(null, lineCode, "%" + rate.stripTrailingZeros().toPlainString() + " KDV'li satışlar",
                    entry.getValue(), salesVatByRate.getOrDefault(rate, BigDecimal.ZERO), order++));
        }
        lines.add(buildLine(null, "60", "Toplam satış matrahı", totalSalesBase, null, order++));

        // Tablo II — İndirilecek KDV
        lines.add(buildLine(null, "T2_100", "Bu döneme ait indirilecek KDV", null, totalPurchasesVat, order++));
        lines.add(buildLine(null, "T2_105", "Devreden KDV", null, carryover, order++));

        Company company = companyRepository.getReferenceById(companyId);

        VatDeclaration declaration = VatDeclaration.builder()
                .company(company)
                .declarationType(KDV1)
                .year(year)
                .month(month)
                .status(DeclarationStatus.DRAFT)
                .totalSalesBase(totalSalesBase)
                .totalSalesVat(totalSalesVat)
                .totalPurchasesBase(totalPurchasesBase)
                .totalPurchasesVat(totalPurchasesVat)
                .previousPeriodCarryover(carryover)
                .payableVat(payableVat)
                .nextPeriodCarryover(nextPeriodCarryover)
                .generatedAt(LocalDateTime.now())
                .build();

        lines.forEach(l -> l.setDeclaration(declaration));
        declaration.setLines(lines);

        String xml = VatDeclarationXmlBuilder.build(declaration, company);
        declaration.setXmlContent(xml);

        VatDeclaration saved = declarationRepository.save(declaration);
        return toDto(saved);
    }

    public VatDeclarationResponseDto finalize(Long declarationId) {
        VatDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.DRAFT) {
            throw new BusinessException("Yalnızca DRAFT durumundaki beyannameler onaylanabilir");
        }
        decl.setStatus(DeclarationStatus.FINALIZED);
        return toDto(declarationRepository.save(decl));
    }

    public VatDeclarationResponseDto markSubmitted(Long declarationId) {
        VatDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.FINALIZED) {
            throw new BusinessException("Yalnızca FINALIZED durumundaki beyannameler gönderilmiş olarak işaretlenebilir");
        }
        decl.setStatus(DeclarationStatus.SUBMITTED);
        decl.setSubmittedAt(LocalDateTime.now());
        return toDto(declarationRepository.save(decl));
    }

    public List<VatDeclarationResponseDto> list() {
        Long companyId = companyContext.getCurrentCompanyId();
        return declarationRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByYearDescMonthDesc(companyId)
                .stream().map(this::toDto).toList();
    }

    public VatDeclarationResponseDto getById(Long declarationId) {
        return toDto(findByIdAndCompany(declarationId));
    }

    public String getXml(Long declarationId) {
        VatDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getXmlContent() == null) throw new BusinessException("XML henüz üretilmemiş");
        return decl.getXmlContent();
    }

    public void delete(Long declarationId) {
        VatDeclaration decl = findByIdAndCompany(declarationId);
        if (decl.getStatus() != DeclarationStatus.DRAFT) {
            throw new BusinessException("Yalnızca DRAFT durumundaki beyannameler silinebilir");
        }
        decl.setDeleted(true);
        decl.setDeletedAt(LocalDateTime.now());
        declarationRepository.save(decl);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VatDeclaration findByIdAndCompany(Long declarationId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return declarationRepository
                .findByDeclarationIdAndCompanyCompanyIdAndIsDeletedFalse(declarationId, companyId)
                .orElseThrow(() -> new BusinessException("Beyanname bulunamadı: " + declarationId));
    }

    private BigDecimal getPreviousPeriodCarryover(Long companyId, int year, int month) {
        int prevYear = month == 1 ? year - 1 : year;
        int prevMonth = month == 1 ? 12 : month - 1;
        return declarationRepository
                .findByCompanyCompanyIdAndDeclarationTypeAndYearAndMonthAndIsDeletedFalse(companyId, KDV1, prevYear, prevMonth)
                .map(VatDeclaration::getNextPeriodCarryover)
                .orElse(BigDecimal.ZERO);
    }

    private static Map<BigDecimal, BigDecimal> groupBase(List<InvoiceVatSummary> list) {
        Map<BigDecimal, BigDecimal> map = new TreeMap<>();
        for (InvoiceVatSummary s : list) {
            map.merge(s.getVatRate(), s.getVatBaseAmount(), BigDecimal::add);
        }
        return map;
    }

    private static Map<BigDecimal, BigDecimal> groupVat(List<InvoiceVatSummary> list) {
        Map<BigDecimal, BigDecimal> map = new TreeMap<>();
        for (InvoiceVatSummary s : list) {
            map.merge(s.getVatRate(), s.getVatAmount(), BigDecimal::add);
        }
        return map;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String rateToLineCode(BigDecimal rate) {
        int r = rate.intValue();
        return switch (r) {
            case 1  -> "30";
            case 10 -> "35";
            case 20 -> "39";
            default -> "3" + r;
        };
    }

    private static VatDeclarationLine buildLine(VatDeclaration decl, String code, String desc,
                                                 BigDecimal base, BigDecimal vat, int order) {
        return VatDeclarationLine.builder()
                .declaration(decl)
                .lineCode(code)
                .description(desc)
                .baseAmount(base)
                .vatAmount(vat)
                .lineOrder(order)
                .build();
    }

    private VatDeclarationResponseDto toDto(VatDeclaration d) {
        List<VatDeclarationLineDto> lineDtos = d.getLines().stream()
                .map(l -> new VatDeclarationLineDto(
                        l.getLineId(), l.getLineCode(), l.getDescription(),
                        l.getBaseAmount(), l.getVatAmount(), l.getLineOrder()))
                .toList();
        return new VatDeclarationResponseDto(
                d.getDeclarationId(), d.getDeclarationType(), d.getYear(), d.getMonth(),
                d.getStatus(), d.getTotalSalesBase(), d.getTotalSalesVat(),
                d.getTotalPurchasesBase(), d.getTotalPurchasesVat(),
                d.getPreviousPeriodCarryover(), d.getPayableVat(), d.getNextPeriodCarryover(),
                d.getGeneratedAt(), d.getSubmittedAt(), lineDtos);
    }
}
