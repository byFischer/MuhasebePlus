package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CollectionPerformanceExcelBuilder {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);

        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        Map<Long, BigDecimal> openByCustomer = new HashMap<>();
        Map<Long, BigDecimal> over90ByCustomer = new HashMap<>();
        Map<Long, Long> totalDelayDays = new HashMap<>();
        Map<Long, Integer> invoiceCountByCustomer = new HashMap<>();

        BigDecimal b1 = BigDecimal.ZERO, b2 = BigDecimal.ZERO, b3 = BigDecimal.ZERO, b4 = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (Invoice inv : openInvoices) {
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            long days = inv.getDueDate() != null
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                    : 0;
            if (days > 0) overdueAmount = overdueAmount.add(amount);
            if (days <= 30) b1 = b1.add(amount);
            else if (days <= 60) b2 = b2.add(amount);
            else if (days <= 90) b3 = b3.add(amount);
            else b4 = b4.add(amount);

            Long cid = inv.getCustomerId();
            if (cid == null) continue;
            openByCustomer.merge(cid, amount, BigDecimal::add);
            if (days > 90) over90ByCustomer.merge(cid, amount, BigDecimal::add);
            totalDelayDays.merge(cid, Math.max(0, days), (a, b) -> a + b);
            invoiceCountByCustomer.merge(cid, 1, (a, b) -> a + b);
        }

        BigDecimal endingAR = b1.add(b2).add(b3).add(b4);
        BigDecimal currentAR = b1;

        BigDecimal creditSales = ExcelAggregationUtils.sumInvoices(invoiceRepository
                .findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().toLocalDate().isBefore(start)
                        && !i.getCreatedAt().toLocalDate().isAfter(end))
                .toList());

        LocalDateTime startDt = start.atStartOfDay();
        BigDecimal beginningAR = ExcelAggregationUtils.sumInvoices(openInvoices.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isBefore(startDt))
                .toList());

        BigDecimal avgAR = beginningAR.add(endingAR).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal dso = creditSales.signum() > 0
                ? avgAR.multiply(BigDecimal.valueOf(periodDays)).divide(creditSales, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal arTurnover = avgAR.signum() > 0
                ? creditSales.divide(avgAR, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal overduePct = endingAR.signum() > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100)).divide(endingAR, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal numerator = beginningAR.add(creditSales).subtract(endingAR);
        BigDecimal denominator = beginningAR.add(creditSales).subtract(currentAR);
        BigDecimal cei = denominator.signum() > 0
                ? numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 1, "Rapor Tarihi:", today.toString());
        ExcelStyleUtils.writeRow(summary, 3, "DSO (gün)", dso.toPlainString());
        ExcelStyleUtils.writeRow(summary, 4, "A/R Turnover", arTurnover.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Overdue %", overduePct.toPlainString());
        ExcelStyleUtils.writeRow(summary, 6, "CEI", cei.toPlainString());
        ExcelStyleUtils.writeRow(summary, 8, "Açık AR (toplam)", endingAR.toPlainString());
        ExcelStyleUtils.writeRow(summary, 9, "Vadesi Geçmiş Tutar", overdueAmount.toPlainString());
        ExcelStyleUtils.writeRow(summary, 10, "Dönem Kredili Satış", creditSales.toPlainString());

        ExcelStyleUtils.writeHeaderRow(summary, 12, bold, "Bucket", "Tutar");
        Row r1 = summary.createRow(13); r1.createCell(0).setCellValue("0-30 gün");  r1.createCell(1).setCellValue(b1.doubleValue());
        Row r2 = summary.createRow(14); r2.createCell(0).setCellValue("31-60 gün"); r2.createCell(1).setCellValue(b2.doubleValue());
        Row r3 = summary.createRow(15); r3.createCell(0).setCellValue("61-90 gün"); r3.createCell(1).setCellValue(b3.doubleValue());
        Row r4 = summary.createRow(16); r4.createCell(0).setCellValue("90+ gün");   r4.createCell(1).setCellValue(b4.doubleValue());
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet detail = wb.createSheet("Müşteri Performansı");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold,
                "Müşteri", "Açık Tutar", "90+ Tutar", "Ort. Gecikme (gün)", "Risk Skoru");
        Map<Long, String> nameMap = new HashMap<>();
        if (!openByCustomer.isEmpty()) {
            customerRepository.findAllById(openByCustomer.keySet())
                    .forEach(c -> nameMap.put(c.getCustomerId(), c.getName()));
        }
        BigDecimal endingARForRisk = endingAR.signum() > 0 ? endingAR : BigDecimal.ONE;
        int rr = 1;
        List<Map.Entry<Long, BigDecimal>> sorted = new ArrayList<>(openByCustomer.entrySet());
        sorted.sort((a, c) -> c.getValue().compareTo(a.getValue()));
        for (Map.Entry<Long, BigDecimal> e : sorted) {
            Long cid = e.getKey();
            BigDecimal open = e.getValue();
            BigDecimal over90 = over90ByCustomer.getOrDefault(cid, BigDecimal.ZERO);
            int count = invoiceCountByCustomer.getOrDefault(cid, 1);
            long avgDelay = totalDelayDays.getOrDefault(cid, 0L) / Math.max(1, count);
            BigDecimal share = open.multiply(BigDecimal.valueOf(100)).divide(endingARForRisk, 1, RoundingMode.HALF_UP);
            String risk = share.compareTo(BigDecimal.valueOf(20)) >= 0 ? "A (Yüksek)"
                        : share.compareTo(BigDecimal.valueOf(10)) >= 0 ? "B (Orta)"
                        : "C (Düşük)";
            Row row = detail.createRow(rr++);
            row.createCell(0).setCellValue(nameMap.getOrDefault(cid, "Müşteri #" + cid));
            row.createCell(1).setCellValue(open.doubleValue());
            row.createCell(2).setCellValue(over90.doubleValue());
            row.createCell(3).setCellValue(avgDelay);
            row.createCell(4).setCellValue(risk);
        }
        ExcelStyleUtils.autoSize(detail, 5);
    }
}
