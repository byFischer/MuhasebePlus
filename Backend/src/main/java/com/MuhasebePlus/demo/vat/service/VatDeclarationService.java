package com.MuhasebePlus.demo.vat.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.vat.dto.BaBsEntryDto;
import com.MuhasebePlus.demo.vat.dto.VatCalculateRequestDto;
import com.MuhasebePlus.demo.vat.dto.VatDeclarationLineDto;
import com.MuhasebePlus.demo.vat.dto.VatPeriodResponseDto;
import com.MuhasebePlus.demo.vat.entity.VatDeclarationLine;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import com.MuhasebePlus.demo.vat.repository.VatDeclarationLineRepository;
import com.MuhasebePlus.demo.vat.repository.VatPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VatDeclarationService {

    private static final BigDecimal BA_BS_THRESHOLD = new BigDecimal("5000");

    private final VatPeriodRepository vatPeriodRepository;
    private final VatDeclarationLineRepository vatDeclarationLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final AccountingPeriodGuard periodGuard;
    private final JournalEntryService journalEntryService;

    @Transactional(readOnly = true)
    public List<VatPeriodResponseDto> getAll() {
        Long companyId = companyContext.getCurrentCompanyId();
        return vatPeriodRepository.findByCompanyCompanyIdOrderByYearDescMonthDesc(companyId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VatPeriodResponseDto getById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        VatPeriod period = vatPeriodRepository.findByIdAndCompanyCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("KDV dönemi bulunamadı: " + id));
        return toDto(period);
    }

    public VatPeriodResponseDto calculate(VatCalculateRequestDto req) {
        Long companyId = companyContext.getCurrentCompanyId();
        int year = req.year();
        int month = req.month();

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        VatPeriod existing = vatPeriodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, year, month)
                .orElse(null);
        if (existing != null && "LOCKED".equalsIgnoreCase(existing.getStatus())) {
            throw new BusinessException(String.format("%d/%02d KDV donemi kilitli, yeniden hesaplanamaz.", year, month));
        }

        List<Invoice> invoices = invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId)
                .stream()
                .filter(inv -> !inv.isCancelled()
                        && inv.getPaymentStatus() != PaymentStatus.draft
                        && inv.getInvoiceDate() != null
                        && !inv.getInvoiceDate().isBefore(startDate)
                        && !inv.getInvoiceDate().isAfter(endDate))
                .toList();

        // Group line items by VAT rate → OUTPUT (satış) and INPUT (alış)
        Map<Integer, BigDecimal[]> outputByRate = new TreeMap<>(); // rate -> [base, vat]
        Map<Integer, BigDecimal[]> inputByRate = new TreeMap<>();

        for (Invoice inv : invoices) {
            List<InvoiceLineItem> lineItems = invoiceLineItemRepository
                    .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
            boolean isSale = InvoiceType.sale.equals(inv.getInvoiceType());
            Map<Integer, BigDecimal[]> targetMap = isSale ? outputByRate : inputByRate;

            for (InvoiceLineItem li : lineItems) {
                int rate = li.getVatRate() == null ? 0 : li.getVatRate().intValue();
                BigDecimal base = taxableBase(li);
                BigDecimal vat = vatAmount(base, li.getVatRate());

                targetMap.computeIfAbsent(rate, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                targetMap.get(rate)[0] = targetMap.get(rate)[0].add(base);
                targetMap.get(rate)[1] = targetMap.get(rate)[1].add(vat);
            }
        }

        BigDecimal totalOutput = outputByRate.values().stream()
                .map(arr -> arr[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInput = inputByRate.values().stream()
                .map(arr -> arr[1]).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal carried = previousCarriedForward(companyId, ym, req.previousCarriedForward());
        BigDecimal netPayable = totalOutput.subtract(totalInput).subtract(carried);
        BigDecimal newCarried = BigDecimal.ZERO;
        if (netPayable.compareTo(BigDecimal.ZERO) < 0) {
            newCarried = netPayable.negate();
            netPayable = BigDecimal.ZERO;
        }

        // Upsert VatPeriod
        VatPeriod period = existing != null
                ? existing
                : vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(companyId, year, month)
                .orElseGet(() -> {
                    VatPeriod p = new VatPeriod();
                    p.setCompany(companyRepository.getReferenceById(companyId));
                    p.setYear(year);
                    p.setMonth(month);
                    return p;
                });

        period.setTotalOutputVat(round2(totalOutput));
        period.setTotalInputVat(round2(totalInput));
        period.setPreviousCarriedForwardVat(round2(carried));
        period.setNetPayableVat(round2(netPayable));
        period.setCarriedForwardVat(round2(newCarried));
        period.setStatus("DRAFT");
        period = vatPeriodRepository.save(period);

        // Rebuild lines
        vatDeclarationLineRepository.deleteByVatPeriodId(period.getId());
        List<VatDeclarationLine> lines = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal[]> e : outputByRate.entrySet()) {
            lines.add(VatDeclarationLine.builder()
                    .vatPeriod(period).lineType("OUTPUT")
                    .vatRate(e.getKey()).taxBase(round2(e.getValue()[0])).vatAmount(round2(e.getValue()[1])).build());
        }
        for (Map.Entry<Integer, BigDecimal[]> e : inputByRate.entrySet()) {
            lines.add(VatDeclarationLine.builder()
                    .vatPeriod(period).lineType("INPUT")
                    .vatRate(e.getKey()).taxBase(round2(e.getValue()[0])).vatAmount(round2(e.getValue()[1])).build());
        }
        vatDeclarationLineRepository.saveAll(lines);
        period.setLines(lines);

        log.info("VAT period calculated companyId={} year={} month={} outputVat={} inputVat={}",
                companyId, year, month, totalOutput, totalInput);
        return toDto(period);
    }

    public VatPeriodResponseDto lock(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        VatPeriod period = vatPeriodRepository.findByIdAndCompanyCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("KDV dönemi bulunamadı: " + id));
        if ("LOCKED".equalsIgnoreCase(period.getStatus())) {
            throw new BusinessException("KDV donemi zaten kilitli.");
        }

        LocalDate periodEnd = YearMonth.of(period.getYear(), period.getMonth()).atEndOfMonth();
        periodGuard.assertOpen(periodEnd);
        Long journalEntryId = journalEntryService.createForVatSettlement(period);
        if (journalEntryId != null) {
            period.setSettlementJournalEntryId(journalEntryId);
        }
        period.setStatus("LOCKED");
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(companyContext.getCurrentUserId());
        return toDto(vatPeriodRepository.save(period));
    }

    @Transactional(readOnly = true)
    public List<BaBsEntryDto> getBaBs(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        VatPeriod period = vatPeriodRepository.findByIdAndCompanyCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("KDV dönemi bulunamadı: " + id));

        YearMonth ym = YearMonth.of(period.getYear(), period.getMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Invoice> invoices = invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId)
                .stream()
                .filter(inv -> !inv.isCancelled()
                        && inv.getInvoiceDate() != null
                        && !inv.getInvoiceDate().isBefore(startDate)
                        && !inv.getInvoiceDate().isAfter(endDate))
                .toList();

        // Aggregate by customer
        Map<Long, BigDecimal> saleTotals = new HashMap<>();
        Map<Long, BigDecimal> purchaseTotals = new HashMap<>();

        for (Invoice inv : invoices) {
            if (inv.getCustomerId() == null) continue;
            if (InvoiceType.sale.equals(inv.getInvoiceType())) {
                saleTotals.merge(inv.getCustomerId(),
                        inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
            } else {
                purchaseTotals.merge(inv.getCustomerId(),
                        inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        List<BaBsEntryDto> result = new ArrayList<>();
        // BS = satış (biz sattık, karşı taraf aldı)
        saleTotals.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BA_BS_THRESHOLD) >= 0)
                .forEach(e -> customerRepository.findById(e.getKey()).ifPresent(c ->
                        result.add(new BaBsEntryDto(c.getCustomerId(), c.getName(),
                                c.getTaxNumber(), e.getValue(), BigDecimal.ZERO, "BS"))));

        // BA = alış (biz aldık, karşı taraf sattı)
        purchaseTotals.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BA_BS_THRESHOLD) >= 0)
                .forEach(e -> customerRepository.findById(e.getKey()).ifPresent(c ->
                        result.add(new BaBsEntryDto(c.getCustomerId(), c.getName(),
                                c.getTaxNumber(), e.getValue(), BigDecimal.ZERO, "BA"))));

        return result;
    }

    private VatPeriodResponseDto toDto(VatPeriod p) {
        List<VatDeclarationLineDto> lineDtos = p.getLines().stream()
                .map(l -> new VatDeclarationLineDto(l.getLineType(), l.getVatRate(), l.getTaxBase(), l.getVatAmount()))
                .toList();
        return new VatPeriodResponseDto(
                p.getId(), p.getYear(), p.getMonth(), p.getStatus(),
                p.getTotalOutputVat(), p.getTotalInputVat(),
                p.getNetPayableVat(), p.getPreviousCarriedForwardVat(), p.getCarriedForwardVat(),
                p.getSettlementJournalEntryId(), p.getLockedAt(), p.getLockedBy(),
                lineDtos, p.getCreatedAt(), p.getUpdatedAt());
    }

    private BigDecimal taxableBase(InvoiceLineItem line) {
        BigDecimal rate = line.getVatRate() != null ? line.getVatRate() : BigDecimal.ZERO;
        if (line.getUnitPrice() != null && line.getQuantity() != null) {
            BigDecimal grossBase = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            BigDecimal discountRate = line.getDiscountRate() != null ? line.getDiscountRate() : BigDecimal.ZERO;
            BigDecimal discount = grossBase.multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return round2(grossBase.subtract(discount));
        }

        BigDecimal lineTotal = line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO;
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return round2(lineTotal);
        }
        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return round2(lineTotal.divide(divisor, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal vatAmount(BigDecimal base, BigDecimal vatRate) {
        BigDecimal rate = vatRate != null ? vatRate : BigDecimal.ZERO;
        return round2(base.multiply(rate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal previousCarriedForward(Long companyId, YearMonth period, BigDecimal explicitValue) {
        if (explicitValue != null) {
            return round2(explicitValue);
        }
        YearMonth previous = period.minusMonths(1);
        return vatPeriodRepository
                .findByCompanyCompanyIdAndYearAndMonth(companyId, previous.getYear(), previous.getMonthValue())
                .map(VatPeriod::getCarriedForwardVat)
                .map(this::round2)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal round2(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
