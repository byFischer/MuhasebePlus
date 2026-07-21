package com.MuhasebePlus.demo.report.service.excel.util;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncomeExpenseSheetWriter {

    public void writeIncomeSheet(Sheet sheet, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        writeIncomeSheetAt(sheet, 0, sales, incomes, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }

    public void writeIncomeSheetAt(Sheet sheet, int startRow, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        int r = startRow;

        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Satış - Ödenmiş)"); t1.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Fatura No", "Müşteri ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : sales) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row salesTotal = sheet.createRow(r++);
        Cell s0 = salesTotal.createCell(0); s0.setCellValue("Toplam"); s0.setCellStyle(bold);
        Cell s1 = salesTotal.createCell(3); s1.setCellValue(ExcelAggregationUtils.sumInvoices(sales).doubleValue()); s1.setCellStyle(bold);

        r++;

        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (INCOME)"); t2.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : incomes) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(ExcelAggregationUtils.sumTransactions(incomes).doubleValue()); tt1.setCellStyle(bold);
    }

    public void writeExpenseSheet(Sheet sheet, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        writeExpenseSheetAt(sheet, 0, purchases, expenses, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }

    public void writeExpenseSheetAt(Sheet sheet, int startRow, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        int r = startRow;

        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Alış - Ödenmiş)"); t1.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Fatura No", "Tedarikçi ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : purchases) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row purchaseTotal = sheet.createRow(r++);
        Cell p0 = purchaseTotal.createCell(0); p0.setCellValue("Toplam"); p0.setCellStyle(bold);
        Cell p1 = purchaseTotal.createCell(3); p1.setCellValue(ExcelAggregationUtils.sumInvoices(purchases).doubleValue()); p1.setCellStyle(bold);

        r++;

        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (EXPENSE)"); t2.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : expenses) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(ExcelAggregationUtils.sumTransactions(expenses).doubleValue()); tt1.setCellStyle(bold);
    }
}
