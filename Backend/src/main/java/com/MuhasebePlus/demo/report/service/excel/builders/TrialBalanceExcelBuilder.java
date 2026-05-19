package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrialBalanceExcelBuilder implements ReportExcelBuilder {

    private final JournalEntryService journalEntryService;

    @Override
    public ReportType supports() { return ReportType.TRIAL_BALANCE; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        Sheet sheet = wb.createSheet("Mizan");
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeHeaderRow(sheet, 2, bold, "Hesap Kodu", "Hesap Adı", "Tür", "Borç Toplamı", "Alacak Toplamı", "Bakiye");

        int r = 3;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (TrialBalanceRowDto row : rows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.accountType() != null ? row.accountType().name() : "");
            exRow.createCell(3).setCellValue(row.totalDebit().doubleValue());
            exRow.createCell(4).setCellValue(row.totalCredit().doubleValue());
            exRow.createCell(5).setCellValue(row.balance().doubleValue());
            totalDebit = totalDebit.add(row.totalDebit());
            totalCredit = totalCredit.add(row.totalCredit());
        }
        r++;
        Row totalsRow = sheet.createRow(r);
        Cell tc = totalsRow.createCell(0); tc.setCellValue("TOPLAM"); tc.setCellStyle(bold);
        Cell td = totalsRow.createCell(3); td.setCellValue(totalDebit.doubleValue()); td.setCellStyle(bold);
        Cell tc2 = totalsRow.createCell(4); tc2.setCellValue(totalCredit.doubleValue()); tc2.setCellStyle(bold);
        BigDecimal diff = totalDebit.subtract(totalCredit);
        Cell tbal = totalsRow.createCell(5); tbal.setCellValue(diff.doubleValue()); tbal.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 6);
    }
}
