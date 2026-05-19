package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.Budget;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.repository.BudgetRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BudgetVarianceExcelBuilder implements ReportExcelBuilder {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public ReportType supports() { return ReportType.BUDGET_VARIANCE; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Budget> allBudgets = budgetRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId);
        List<Budget> periodBudgets = allBudgets.stream()
                .filter(b -> {
                    LocalDate budgetMonth = LocalDate.of(b.getYear(), b.getMonth(), 1);
                    LocalDate budgetEnd = budgetMonth.withDayOfMonth(budgetMonth.lengthOfMonth());
                    return !budgetEnd.isBefore(start) && !budgetMonth.isAfter(end);
                })
                .toList();

        List<Transaction> allTx = transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(start, end, companyId);

        Map<String, BigDecimal> plannedByCategory = new HashMap<>();
        for (Budget b : periodBudgets) {
            String cat = b.getCategory() != null ? b.getCategory() : "Diğer";
            plannedByCategory.merge(cat, b.getPlannedAmount() != null ? b.getPlannedAmount() : BigDecimal.ZERO, BigDecimal::add);
        }
        Map<String, BigDecimal> actualByCategory = new HashMap<>();
        for (Transaction tx : allTx) {
            String cat = tx.getCategory() != null && !tx.getCategory().isBlank() ? tx.getCategory() : "Diğer";
            actualByCategory.merge(cat, tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalPlanned = plannedByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActual = actualByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVariance = totalActual.subtract(totalPlanned);
        BigDecimal variancePct = totalPlanned.signum() > 0
                ? totalVariance.abs().multiply(BigDecimal.valueOf(100)).divide(totalPlanned, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        Sheet summary = wb.createSheet("Özet");
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Toplam Plan",        totalPlanned.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Toplam Gerçekleşen", totalActual.toPlainString());
        ExcelStyleUtils.writeRow(summary, 4, "Toplam Sapma",        totalVariance.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Sapma %",             variancePct.toPlainString());
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet detail = wb.createSheet("Kategori Detayı");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold, "Kategori", "Plan", "Gerçekleşen", "Sapma Tutarı", "Sapma %", "Durum");
        Set<String> allCats = new LinkedHashSet<>();
        allCats.addAll(plannedByCategory.keySet());
        allCats.addAll(actualByCategory.keySet());
        int r = 1;
        for (String cat : allCats) {
            BigDecimal pl = plannedByCategory.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal ac = actualByCategory.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal var = ac.subtract(pl);
            BigDecimal pct = pl.signum() > 0
                    ? var.abs().multiply(BigDecimal.valueOf(100)).divide(pl, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String status = pct.compareTo(BigDecimal.valueOf(20)) > 0 ? "Kontrol Dışı" : "Normal";
            Row row = detail.createRow(r++);
            row.createCell(0).setCellValue(cat);
            row.createCell(1).setCellValue(pl.doubleValue());
            row.createCell(2).setCellValue(ac.doubleValue());
            row.createCell(3).setCellValue(var.doubleValue());
            row.createCell(4).setCellValue(pct.doubleValue());
            row.createCell(5).setCellValue(status);
        }
        ExcelStyleUtils.autoSize(detail, 6);
    }
}
