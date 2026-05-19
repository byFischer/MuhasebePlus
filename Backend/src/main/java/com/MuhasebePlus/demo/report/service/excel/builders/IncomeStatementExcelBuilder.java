package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
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
public class IncomeStatementExcelBuilder {

    private final JournalEntryService journalEntryService;

    public void build(Workbook wb, LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        List<TrialBalanceRowDto> incomeRows = rows.stream()
                .filter(r -> r.accountType() == AccountType.INCOME).toList();
        List<TrialBalanceRowDto> expenseRows = rows.stream()
                .filter(r -> r.accountType() == AccountType.EXPENSE || r.accountType() == AccountType.COST).toList();

        BigDecimal totalIncome = incomeRows.stream()
                .map(r -> r.balance().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenseRows.stream()
                .map(TrialBalanceRowDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        Sheet sheet = wb.createSheet("Gelir Tablosu");
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);

        int r = 2;
        Row h1 = sheet.createRow(r++); Cell hc1 = h1.createCell(0); hc1.setCellValue("GELİRLER"); hc1.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adı", "Tutar");
        for (TrialBalanceRowDto row : incomeRows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.balance().negate().doubleValue());
        }
        Row incTotalRow = sheet.createRow(r++);
        Cell itc = incTotalRow.createCell(1); itc.setCellValue("Toplam Gelir"); itc.setCellStyle(bold);
        Cell itv = incTotalRow.createCell(2); itv.setCellValue(totalIncome.doubleValue()); itv.setCellStyle(bold);

        r++;
        Row h2 = sheet.createRow(r++); Cell hc2 = h2.createCell(0); hc2.setCellValue("GİDERLER / MALİYETLER"); hc2.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adı", "Tutar");
        for (TrialBalanceRowDto row : expenseRows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.balance().doubleValue());
        }
        Row expTotalRow = sheet.createRow(r++);
        Cell etc = expTotalRow.createCell(1); etc.setCellValue("Toplam Gider"); etc.setCellStyle(bold);
        Cell etv = expTotalRow.createCell(2); etv.setCellValue(totalExpense.doubleValue()); etv.setCellStyle(bold);

        r++;
        Row netRow = sheet.createRow(r);
        Cell nc = netRow.createCell(1); nc.setCellValue("NET KÂR / ZARAR"); nc.setCellStyle(bold);
        Cell nv = netRow.createCell(2); nv.setCellValue(netProfit.doubleValue()); nv.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 3);
    }
}
