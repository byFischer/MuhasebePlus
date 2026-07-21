package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CashFlowExcelBuilder implements ReportExcelBuilder {

    private final ReportDataFetcher fetcher;

    @Override
    public ReportType supports() { return ReportType.CASH_FLOW; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);
        BigDecimal inflow = ExcelAggregationUtils.sumTransactions(incomes);
        BigDecimal outflow = ExcelAggregationUtils.sumTransactions(expenses);
        BigDecimal net = inflow.subtract(outflow);

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Toplam Giriş", inflow.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Toplam Çıkış", outflow.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0); c0.setCellValue("Net Nakit Akışı"); c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1); c1.setCellValue(net.toPlainString()); c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet sheet = wb.createSheet("İşlemler");
        ExcelStyleUtils.writeHeaderRow(sheet, 0, bold, "Tarih", "Hesap ID", "Tip", "Açıklama", "Giriş", "Çıkış", "Kümülatif Bakiye");
        List<Transaction> all = new ArrayList<>();
        all.addAll(incomes);
        all.addAll(expenses);
        all.sort((a, b) -> {
            LocalDate da = a.getTransactionDate();
            LocalDate db = b.getTransactionDate();
            if (da == null && db == null) return 0;
            if (da == null) return -1;
            if (db == null) return 1;
            return da.compareTo(db);
        });
        BigDecimal running = BigDecimal.ZERO;
        int r = 1;
        for (Transaction tx : all) {
            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            boolean isIn = tx.getTransactionType() == TransactionType.INCOME;
            running = isIn ? running.add(amt) : running.subtract(amt);
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(isIn ? "GİRİŞ" : "ÇIKIŞ");
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(isIn ? amt.doubleValue() : 0);
            row.createCell(5).setCellValue(!isIn ? amt.doubleValue() : 0);
            row.createCell(6).setCellValue(running.doubleValue());
        }
        ExcelStyleUtils.autoSize(sheet, 7);
    }
}
