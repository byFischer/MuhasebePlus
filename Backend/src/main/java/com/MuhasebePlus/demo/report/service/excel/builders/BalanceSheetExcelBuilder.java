package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementLineDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementSectionDto;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
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

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BalanceSheetExcelBuilder implements ReportExcelBuilder {

    private final JournalEntryService journalEntryService;

    @Override
    public ReportType supports() { return ReportType.BALANCE_SHEET; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        BalanceSheetResponseDto balanceSheet = journalEntryService.getBalanceSheet(end);
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        Sheet sheet = wb.createSheet("Bilanco");
        ExcelStyleUtils.writeRow(sheet, 0, "Bilanco Tarihi:", String.valueOf(end));

        int r = 2;
        r = writeSections(sheet, r, bold, "AKTIF (Varliklar)", balanceSheet.assetSections());
        r++;
        r = writeSections(sheet, r, bold, "PASIF (Yabanci Kaynaklar)", balanceSheet.liabilitySections());
        r++;
        r = writeSections(sheet, r, bold, "OZKAYNAKLAR", balanceSheet.equitySections());

        r++;
        Row summaryRow = sheet.createRow(r++);
        Cell activeLabel = summaryRow.createCell(0);
        activeLabel.setCellValue("AKTIF TOPLAMI");
        activeLabel.setCellStyle(bold);
        Cell activeValue = summaryRow.createCell(2);
        activeValue.setCellValue(balanceSheet.totalAssets().doubleValue());
        activeValue.setCellStyle(bold);

        Row summaryRow2 = sheet.createRow(r++);
        Cell passiveLabel = summaryRow2.createCell(0);
        passiveLabel.setCellValue("PASIF TOPLAMI");
        passiveLabel.setCellStyle(bold);
        Cell passiveValue = summaryRow2.createCell(2);
        passiveValue.setCellValue(balanceSheet.totalLiabilitiesAndEquity().doubleValue());
        passiveValue.setCellStyle(bold);

        Row differenceRow = sheet.createRow(r);
        Cell differenceLabel = differenceRow.createCell(0);
        differenceLabel.setCellValue("DENGE FARKI");
        differenceLabel.setCellStyle(bold);
        Cell differenceValue = differenceRow.createCell(2);
        differenceValue.setCellValue(balanceSheet.difference().doubleValue());
        differenceValue.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 3);
    }

    private int writeSections(
            Sheet sheet,
            int rowIndex,
            CellStyle bold,
            String title,
            List<FinancialStatementSectionDto> sections) {
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(bold);

        for (FinancialStatementSectionDto section : sections) {
            Row sectionRow = sheet.createRow(rowIndex++);
            Cell sectionCell = sectionRow.createCell(0);
            sectionCell.setCellValue(section.sectionName());
            sectionCell.setCellStyle(bold);
            ExcelStyleUtils.writeHeaderRow(sheet, rowIndex++, bold, "Hesap Kodu", "Hesap Adi", "Tutar");

            for (FinancialStatementLineDto line : section.lines()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(line.accountCode()));
                row.createCell(1).setCellValue(ExcelStyleUtils.safeStr(line.accountName()));
                row.createCell(2).setCellValue(line.amount().doubleValue());
            }

            Row totalRow = sheet.createRow(rowIndex++);
            Cell totalLabel = totalRow.createCell(1);
            totalLabel.setCellValue("Toplam " + section.sectionName());
            totalLabel.setCellStyle(bold);
            Cell totalValue = totalRow.createCell(2);
            totalValue.setCellValue(section.totalAmount().doubleValue());
            totalValue.setCellStyle(bold);
            rowIndex++;
        }
        return rowIndex;
    }
}
