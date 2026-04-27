package com.MuhasebePlus.demo.report.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.entity.ReportType;

@Component
public class ReportExcelBuilder {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public void build(ReportType type, Long companyId, LocalDate start, LocalDate end, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (type) {
                case PROFIT_LOSS -> buildProfitLoss(wb, companyId, start, end);
                case INCOME -> buildIncome(wb, companyId, start, end);
                case EXPENSE -> buildExpense(wb, companyId, start, end);
            }
            wb.write(out);
        }
    }


    // PROFIT/LOSS

    private void buildProfitLoss(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incomes = fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        BigDecimal totalRevenue = sumInvoices(paidSales).add(sumTransactions(incomes));
        BigDecimal totalExpense = sumInvoices(paidPurchases).add(sumTransactions(expenses));
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        // Sheet 1: Ozet
        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = boldStyle(wb);
        writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        writeRow(summary, 2, "Toplam Gelir", totalRevenue.toPlainString());
        writeRow(summary, 3, "Toplam Gider", totalExpense.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0); c0.setCellValue("Net Kâr/Zarar"); c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1); c1.setCellValue(netProfit.toPlainString()); c1.setCellStyle(bold);
        autoSize(summary, 2);

        // Sheet 2: Gelirler
        Sheet incomeSheet = wb.createSheet("Gelirler");
        writeIncomeSheet(incomeSheet, paidSales, incomes, bold);

        // Sheet 3: Giderler
        Sheet expenseSheet = wb.createSheet("Giderler");
        writeExpenseSheet(expenseSheet, paidPurchases, expenses, bold);
    }


    // INCOME ONLY

    private void buildIncome(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Transaction> incomes = fetchTransactions(companyId, TransactionType.INCOME, start, end);

        Sheet sheet = wb.createSheet("Gelir Raporu");
        CellStyle bold = boldStyle(wb);
        writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        writeIncomeSheetAt(sheet, 2, paidSales, incomes, bold);
        autoSize(sheet, 5);
    }


    // EXPENSE ONLY

    private void buildExpense(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidPurchases = fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> expenses = fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        Sheet sheet = wb.createSheet("Gider Raporu");
        CellStyle bold = boldStyle(wb);
        writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        writeExpenseSheetAt(sheet, 2, paidPurchases, expenses, bold);
        autoSize(sheet, 5);
    }


    // SHEET WRITERS

    private void writeIncomeSheet(Sheet sheet, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        writeIncomeSheetAt(sheet, 0, sales, incomes, bold);
        autoSize(sheet, 5);
    }

    private void writeIncomeSheetAt(Sheet sheet, int startRow, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        int r = startRow;

        // Faturalar tablosu
        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Satış - Ödenmiş)"); t1.setCellStyle(bold);
        writeHeaderRow(sheet, r++, bold, "Fatura No", "Müşteri ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : sales) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row salesTotal = sheet.createRow(r++);
        Cell s0 = salesTotal.createCell(0); s0.setCellValue("Toplam"); s0.setCellStyle(bold);
        Cell s1 = salesTotal.createCell(3); s1.setCellValue(sumInvoices(sales).doubleValue()); s1.setCellStyle(bold);

        r++; // bos satir

        // Islemler tablosu
        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (INCOME)"); t2.setCellStyle(bold);
        writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : incomes) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(sumTransactions(incomes).doubleValue()); tt1.setCellStyle(bold);
    }

    private void writeExpenseSheet(Sheet sheet, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        writeExpenseSheetAt(sheet, 0, purchases, expenses, bold);
        autoSize(sheet, 5);
    }

    private void writeExpenseSheetAt(Sheet sheet, int startRow, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        int r = startRow;

        // Faturalar tablosu
        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Alış - Ödenmiş)"); t1.setCellStyle(bold);
        writeHeaderRow(sheet, r++, bold, "Fatura No", "Tedarikçi ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : purchases) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row purchaseTotal = sheet.createRow(r++);
        Cell p0 = purchaseTotal.createCell(0); p0.setCellValue("Toplam"); p0.setCellStyle(bold);
        Cell p1 = purchaseTotal.createCell(3); p1.setCellValue(sumInvoices(purchases).doubleValue()); p1.setCellStyle(bold);

        r++;

        // Islemler tablosu
        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (EXPENSE)"); t2.setCellStyle(bold);
        writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : expenses) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(sumTransactions(expenses).doubleValue()); tt1.setCellStyle(bold);
    }


    // DATA FETCHERS

    private List<Invoice> fetchPaidInvoices(Long companyId, InvoiceType type, LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.paid, type, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().isBefore(startDt)
                        && !i.getCreatedAt().isAfter(endDt))
                .collect(Collectors.toList());
    }

    private List<Transaction> fetchTransactions(Long companyId, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                        start, end, companyId)
                .stream()
                .filter(t -> t.getTransactionType() == type)
                .collect(Collectors.toList());
    }


    // HELPERS

    private BigDecimal sumInvoices(List<Invoice> list) {
        return list.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTransactions(List<Transaction> list) {
        return list.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CellStyle boldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void writeHeaderRow(Sheet sheet, int rowNum, CellStyle style, String... headers) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }
}
