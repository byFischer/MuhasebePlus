package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryLineResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JournalListingExcelBuilder implements ReportExcelBuilder {

    private final JournalEntryService journalEntryService;

    @Override
    public ReportType supports() { return ReportType.JOURNAL_LISTING; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<JournalEntryResponseDto> entries = journalEntryService
                .list(org.springframework.data.domain.PageRequest.of(0, 10000))
                .getContent()
                .stream()
                .filter(e -> e.entryDate() != null && !e.entryDate().isBefore(start) && !e.entryDate().isAfter(end))
                .toList();

        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        Sheet sheet = wb.createSheet("Yevmiye Dökümü");
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeHeaderRow(sheet, 2, bold, "Fiş No", "Tarih", "Kaynak", "Hesap Kodu", "Hesap Adı", "Borç", "Alacak", "Açıklama");

        int r = 3;
        for (JournalEntryResponseDto entry : entries) {
            for (JournalEntryLineResponseDto line : entry.lines()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(entry.entryNumber()));
                row.createCell(1).setCellValue(entry.entryDate() != null ? entry.entryDate().toString() : "");
                row.createCell(2).setCellValue(entry.sourceType() != null ? entry.sourceType().name() : "MANUAL");
                row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(line.accountCode()));
                row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(line.accountName()));
                row.createCell(5).setCellValue(line.debitAmount() != null ? line.debitAmount().doubleValue() : 0);
                row.createCell(6).setCellValue(line.creditAmount() != null ? line.creditAmount().doubleValue() : 0);
                row.createCell(7).setCellValue(ExcelStyleUtils.safeStr(line.description()));
            }
        }
        ExcelStyleUtils.autoSize(sheet, 8);
    }
}
