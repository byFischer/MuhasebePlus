package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.service.excel.util.IncomeExpenseSheetWriter;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfitLossExcelBuilder implements ReportExcelBuilder {

    private final ReportDataFetcher fetcher;
    private final IncomeExpenseSheetWriter sheetWriter;

    @Override
    public ReportType supports() { return ReportType.PROFIT_LOSS; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        BigDecimal totalRevenue = ExcelAggregationUtils.sumInvoices(paidSales).add(ExcelAggregationUtils.sumTransactions(incomes));
        BigDecimal totalExpense = ExcelAggregationUtils.sumInvoices(paidPurchases).add(ExcelAggregationUtils.sumTransactions(expenses));
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Toplam Gelir", totalRevenue.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Toplam Gider", totalExpense.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0); c0.setCellValue("Net Kâr/Zarar"); c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1); c1.setCellValue(netProfit.toPlainString()); c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet incomeSheet = wb.createSheet("Gelirler");
        sheetWriter.writeIncomeSheet(incomeSheet, paidSales, incomes, bold);

        Sheet expenseSheet = wb.createSheet("Giderler");
        sheetWriter.writeExpenseSheet(expenseSheet, paidPurchases, expenses, bold);
    }
}
