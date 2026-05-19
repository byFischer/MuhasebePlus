package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BankReconciliationExcelBuilder implements ReportExcelBuilder {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public ReportType supports() { return ReportType.BANK_RECONCILIATION; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Transaction> allTx = transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(start, end, companyId);

        List<BankAccount> accounts = bankAccountRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(companyId);
        Map<Long, String> accountNames = new HashMap<>();
        Map<Long, String> accountIbans = new HashMap<>();
        accounts.forEach(a -> {
            accountNames.put(a.getAccountId(), a.getBankName() != null ? a.getBankName() : "Hesap #" + a.getAccountId());
            accountIbans.put(a.getAccountId(), a.getIban() != null ? a.getIban() : "");
        });

        Map<Long, BigDecimal> inByAccount = new HashMap<>();
        Map<Long, BigDecimal> outByAccount = new HashMap<>();
        for (Transaction tx : allTx) {
            if (tx.getAccountId() == null) continue;
            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getTransactionType() == TransactionType.INCOME) inByAccount.merge(tx.getAccountId(), amt, BigDecimal::add);
            else outByAccount.merge(tx.getAccountId(), amt, BigDecimal::add);
        }

        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        Sheet balSheet = wb.createSheet("Hesap Bakiyeleri");
        ExcelStyleUtils.writeHeaderRow(balSheet, 0, bold, "Hesap", "IBAN", "Toplam Giriş", "Toplam Çıkış", "Net Bakiye");
        int r = 1;
        for (BankAccount acc : accounts) {
            BigDecimal in = inByAccount.getOrDefault(acc.getAccountId(), BigDecimal.ZERO);
            BigDecimal out = outByAccount.getOrDefault(acc.getAccountId(), BigDecimal.ZERO);
            Row row = balSheet.createRow(r++);
            row.createCell(0).setCellValue(accountNames.get(acc.getAccountId()));
            row.createCell(1).setCellValue(accountIbans.get(acc.getAccountId()));
            row.createCell(2).setCellValue(in.doubleValue());
            row.createCell(3).setCellValue(out.doubleValue());
            row.createCell(4).setCellValue(in.subtract(out).doubleValue());
        }
        ExcelStyleUtils.autoSize(balSheet, 5);

        Sheet dupSheet = wb.createSheet("Şüpheli Çift Kayıt");
        ExcelStyleUtils.writeHeaderRow(dupSheet, 0, bold, "Tarih", "Hesap", "Tip", "Tutar", "Açıklama", "Tekrar Sayısı");
        Map<String, List<Transaction>> dupGroups = new HashMap<>();
        for (Transaction tx : allTx) {
            String key = tx.getAccountId() + "|" + tx.getTransactionDate() + "|" + tx.getAmount() + "|" + tx.getTransactionType();
            dupGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }
        int rr = 1;
        for (Map.Entry<String, List<Transaction>> e : dupGroups.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            Transaction first = e.getValue().get(0);
            Row row = dupSheet.createRow(rr++);
            row.createCell(0).setCellValue(first.getTransactionDate() != null ? first.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(accountNames.getOrDefault(first.getAccountId(), "Hesap #" + first.getAccountId()));
            row.createCell(2).setCellValue(first.getTransactionType() != null ? first.getTransactionType().name() : "");
            row.createCell(3).setCellValue(first.getAmount() != null ? first.getAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(first.getDescription()));
            row.createCell(5).setCellValue(e.getValue().size());
        }
        ExcelStyleUtils.autoSize(dupSheet, 6);

        Sheet noDescSheet = wb.createSheet("Açıklamasız Hareketler");
        ExcelStyleUtils.writeHeaderRow(noDescSheet, 0, bold, "Tarih", "Hesap", "Tip", "Tutar", "Kategori");
        int rrr = 1;
        for (Transaction tx : allTx) {
            if (tx.getDescription() != null && !tx.getDescription().isBlank()) continue;
            Row row = noDescSheet.createRow(rrr++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(accountNames.getOrDefault(tx.getAccountId(), "Hesap #" + tx.getAccountId()));
            row.createCell(2).setCellValue(tx.getTransactionType() != null ? tx.getTransactionType().name() : "");
            row.createCell(3).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(tx.getCategory()));
        }
        ExcelStyleUtils.autoSize(noDescSheet, 5);
    }
}
