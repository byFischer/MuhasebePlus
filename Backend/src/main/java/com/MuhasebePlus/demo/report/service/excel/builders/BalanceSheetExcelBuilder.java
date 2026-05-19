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
public class BalanceSheetExcelBuilder {

    private final JournalEntryService journalEntryService;

    public void build(Workbook wb, LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        List<TrialBalanceRowDto> assetRows = rows.stream()
                .filter(r -> r.accountType() == AccountType.ASSET).toList();
        List<TrialBalanceRowDto> liabilityRows = rows.stream()
                .filter(r -> r.accountType() == AccountType.LIABILITY).toList();
        List<TrialBalanceRowDto> equityRows = rows.stream()
                .filter(r -> r.accountType() == AccountType.EQUITY).toList();

        BigDecimal totalAssets = assetRows.stream().map(TrialBalanceRowDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = liabilityRows.stream().map(r -> r.balance().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = equityRows.stream().map(r -> r.balance().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);

        Sheet sheet = wb.createSheet("Bilanço");
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);

        int r = 2;
        Row ah = sheet.createRow(r++); Cell ahc = ah.createCell(0); ahc.setCellValue("AKTİF (Varlıklar)"); ahc.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adı", "Tutar");
        for (TrialBalanceRowDto row : assetRows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.balance().doubleValue());
        }
        Row atRow = sheet.createRow(r++);
        Cell atc = atRow.createCell(1); atc.setCellValue("Toplam Aktif"); atc.setCellStyle(bold);
        Cell atv = atRow.createCell(2); atv.setCellValue(totalAssets.doubleValue()); atv.setCellStyle(bold);

        r++;
        Row ph = sheet.createRow(r++); Cell phc = ph.createCell(0); phc.setCellValue("PASİF (Yabancı Kaynaklar)"); phc.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adı", "Tutar");
        for (TrialBalanceRowDto row : liabilityRows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.balance().negate().doubleValue());
        }
        Row ltRow = sheet.createRow(r++);
        Cell ltc = ltRow.createCell(1); ltc.setCellValue("Toplam Yabancı Kaynaklar"); ltc.setCellStyle(bold);
        Cell ltv = ltRow.createCell(2); ltv.setCellValue(totalLiabilities.doubleValue()); ltv.setCellStyle(bold);

        r++;
        Row eqh = sheet.createRow(r++); Cell eqhc = eqh.createCell(0); eqhc.setCellValue("ÖZ KAYNAKLAR"); eqhc.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Hesap Kodu", "Hesap Adı", "Tutar");
        for (TrialBalanceRowDto row : equityRows) {
            Row exRow = sheet.createRow(r++);
            exRow.createCell(0).setCellValue(ExcelStyleUtils.safeStr(row.accountCode()));
            exRow.createCell(1).setCellValue(ExcelStyleUtils.safeStr(row.accountName()));
            exRow.createCell(2).setCellValue(row.balance().negate().doubleValue());
        }
        Row eqtRow = sheet.createRow(r++);
        Cell eqtc = eqtRow.createCell(1); eqtc.setCellValue("Toplam Öz Kaynaklar"); eqtc.setCellStyle(bold);
        Cell eqtv = eqtRow.createCell(2); eqtv.setCellValue(totalEquity.doubleValue()); eqtv.setCellStyle(bold);

        r++;
        Row summaryRow = sheet.createRow(r++);
        Cell sc1 = summaryRow.createCell(0); sc1.setCellValue("AKTİF TOPLAMI"); sc1.setCellStyle(bold);
        Cell sv1 = summaryRow.createCell(2); sv1.setCellValue(totalAssets.doubleValue()); sv1.setCellStyle(bold);
        Row summaryRow2 = sheet.createRow(r);
        Cell sc2 = summaryRow2.createCell(0); sc2.setCellValue("PASİF TOPLAMI (YK + ÖK)"); sc2.setCellStyle(bold);
        Cell sv2 = summaryRow2.createCell(2); sv2.setCellValue(totalLiabilities.add(totalEquity).doubleValue()); sv2.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 3);
    }
}
