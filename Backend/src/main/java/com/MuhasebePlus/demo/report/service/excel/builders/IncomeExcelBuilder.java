package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.service.excel.util.IncomeExpenseSheetWriter;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IncomeExcelBuilder implements ReportExcelBuilder {

    private final ReportDataFetcher fetcher;
    private final IncomeExpenseSheetWriter sheetWriter;

    @Override
    public ReportType supports() { return ReportType.INCOME; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);

        Sheet sheet = wb.createSheet("Gelir Raporu");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        sheetWriter.writeIncomeSheetAt(sheet, 2, paidSales, incomes, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }
}
