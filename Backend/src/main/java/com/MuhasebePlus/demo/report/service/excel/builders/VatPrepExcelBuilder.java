package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class VatPrepExcelBuilder {

    private final ReportDataFetcher fetcher;

    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> sales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> purchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        BigDecimal collected = ExcelAggregationUtils.sumInvoiceVat(sales);
        BigDecimal paid = ExcelAggregationUtils.sumInvoiceVat(purchases);
        BigDecimal net = collected.subtract(paid);

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Tahsil Edilen KDV (Satış)", collected.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Ödenen KDV (Alış)", paid.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0);
        c0.setCellValue(net.signum() >= 0 ? "Ödenecek KDV" : "İade Edilecek KDV");
        c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1);
        c1.setCellValue(net.abs().toPlainString());
        c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet salesSheet = wb.createSheet("Satış KDV");
        ExcelStyleUtils.writeHeaderRow(salesSheet, 0, bold, "Fatura No", "Müşteri ID", "Tarih", "Matrah", "KDV", "Toplam");
        int r = 1;
        for (Invoice inv : sales) {
            Row row = salesSheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate().toString() : "");
            row.createCell(3).setCellValue(inv.getSubtotal() != null ? inv.getSubtotal().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getVatAmount() != null ? inv.getVatAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
        }
        ExcelStyleUtils.autoSize(salesSheet, 6);

        Sheet purchaseSheet = wb.createSheet("Alış KDV");
        ExcelStyleUtils.writeHeaderRow(purchaseSheet, 0, bold, "Fatura No", "Tedarikçi ID", "Tarih", "Matrah", "KDV", "Toplam");
        r = 1;
        for (Invoice inv : purchases) {
            Row row = purchaseSheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate().toString() : "");
            row.createCell(3).setCellValue(inv.getSubtotal() != null ? inv.getSubtotal().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getVatAmount() != null ? inv.getVatAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
        }
        ExcelStyleUtils.autoSize(purchaseSheet, 6);
    }
}
