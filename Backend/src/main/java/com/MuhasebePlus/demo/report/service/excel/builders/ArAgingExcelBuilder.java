package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArAgingExcelBuilder implements ReportExcelBuilder {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    @Override
    public ReportType supports() { return ReportType.AR_AGING; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        Map<Long, BigDecimal[]> perCustomer = new HashMap<>();
        for (Invoice inv : openInvoices) {
            if (inv.getCustomerId() == null) continue;
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            long days = inv.getDueDate() != null
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                    : 0;
            BigDecimal[] arr = perCustomer.computeIfAbsent(inv.getCustomerId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
            arr[idx] = arr[idx].add(amount);
        }

        Map<Long, String> nameMap = new HashMap<>();
        if (!perCustomer.isEmpty()) {
            customerRepository.findAllById(perCustomer.keySet())
                    .forEach(c -> nameMap.put(c.getCustomerId(), c.getName()));
        }

        BigDecimal t1 = BigDecimal.ZERO, t2 = BigDecimal.ZERO, t3 = BigDecimal.ZERO, t4 = BigDecimal.ZERO;
        for (BigDecimal[] arr : perCustomer.values()) {
            t1 = t1.add(arr[0]); t2 = t2.add(arr[1]); t3 = t3.add(arr[2]); t4 = t4.add(arr[3]);
        }

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih:", today.toString());
        ExcelStyleUtils.writeRow(summary, 2, "0-30 gün", t1.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "31-60 gün", t2.toPlainString());
        ExcelStyleUtils.writeRow(summary, 4, "61-90 gün", t3.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "90+ gün", t4.toPlainString());
        Row tot = summary.createRow(7);
        Cell tc0 = tot.createCell(0); tc0.setCellValue("Toplam Açık Alacak"); tc0.setCellStyle(bold);
        Cell tc1 = tot.createCell(1); tc1.setCellValue(t1.add(t2).add(t3).add(t4).toPlainString()); tc1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet detail = wb.createSheet("Detay");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold, "Müşteri", "0-30", "31-60", "61-90", "90+", "Toplam");
        int r = 1;
        for (Map.Entry<Long, BigDecimal[]> e : perCustomer.entrySet()) {
            BigDecimal[] arr = e.getValue();
            BigDecimal sum = arr[0].add(arr[1]).add(arr[2]).add(arr[3]);
            Row row = detail.createRow(r++);
            row.createCell(0).setCellValue(nameMap.getOrDefault(e.getKey(), "Müşteri #" + e.getKey()));
            row.createCell(1).setCellValue(arr[0].doubleValue());
            row.createCell(2).setCellValue(arr[1].doubleValue());
            row.createCell(3).setCellValue(arr[2].doubleValue());
            row.createCell(4).setCellValue(arr[3].doubleValue());
            row.createCell(5).setCellValue(sum.doubleValue());
        }
        ExcelStyleUtils.autoSize(detail, 6);
    }
}
