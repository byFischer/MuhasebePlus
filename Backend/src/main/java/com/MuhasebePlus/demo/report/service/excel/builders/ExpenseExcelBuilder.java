package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.service.excel.util.IncomeExpenseSheetWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpenseExcelBuilder {

    private final ReportDataFetcher fetcher;
    private final IncomeExpenseSheetWriter sheetWriter;

    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        Sheet sheet = wb.createSheet("Gider Raporu");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        sheetWriter.writeExpenseSheetAt(sheet, 2, paidPurchases, expenses, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }
}
