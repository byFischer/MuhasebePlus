package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementLineDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementSectionDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
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

@Component
@RequiredArgsConstructor
public class IncomeStatementExcelBuilder implements ReportExcelBuilder {

    private final JournalEntryService journalEntryService;

    @Override
    public ReportType supports() { return ReportType.INCOME_STATEMENT; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        IncomeStatementResponseDto statement = journalEntryService.getIncomeStatement(start, end);
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        Sheet sheet = wb.createSheet("Gelir Tablosu");
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Araligi:", start + " - " + end);

        int r = 2;
        for (FinancialStatementSectionDto section : statement.sections()) {
            Row sectionRow = sheet.createRow(r++);
            Cell sectionCell = sectionRow.createCell(0);
            sectionCell.setCellValue(section.sectionName());
            sectionCell.setCellStyle(bold);

            ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adi", "Tutar");
            for (FinancialStatementLineDto line : section.lines()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(line.accountCode()));
                row.createCell(1).setCellValue(ExcelStyleUtils.safeStr(line.accountName()));
                row.createCell(2).setCellValue(line.amount().doubleValue());
            }

            Row totalRow = sheet.createRow(r++);
            Cell totalLabel = totalRow.createCell(1);
            totalLabel.setCellValue("Toplam " + section.sectionName());
            totalLabel.setCellStyle(bold);
            Cell totalValue = totalRow.createCell(2);
            totalValue.setCellValue(section.totalAmount().doubleValue());
            totalValue.setCellStyle(bold);
            r++;
        }

        Row netRow = sheet.createRow(r);
        Cell netLabel = netRow.createCell(1);
        netLabel.setCellValue("NET KAR / ZARAR");
        netLabel.setCellStyle(bold);
        Cell netValue = netRow.createCell(2);
        netValue.setCellValue(statement.netProfit().doubleValue());
        netValue.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 3);
    }
}
