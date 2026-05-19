package com.MuhasebePlus.demo.report.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Budget;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.BudgetRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryLineResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.builders.ArAgingExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.CashFlowExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.CollectionPerformanceExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.ExpenseExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.IncomeExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.ProfitLossExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.SlowInventoryExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.VatPrepExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;

@Component
@RequiredArgsConstructor
public class ReportExcelBuilder {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final BudgetRepository budgetRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalEntryService journalEntryService;
    private final ReportDataFetcher fetcher;
    private final ProfitLossExcelBuilder profitLossBuilder;
    private final IncomeExcelBuilder incomeBuilder;
    private final ExpenseExcelBuilder expenseBuilder;
    private final CashFlowExcelBuilder cashFlowBuilder;
    private final ArAgingExcelBuilder arAgingBuilder;
    private final VatPrepExcelBuilder vatPrepBuilder;
    private final CollectionPerformanceExcelBuilder collectionPerformanceBuilder;
    private final SlowInventoryExcelBuilder slowInventoryBuilder;

    public void build(ReportType type, Long companyId, LocalDate start, LocalDate end, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (type) {
                case PROFIT_LOSS            -> profitLossBuilder.build(wb, companyId, start, end);
                case INCOME                 -> incomeBuilder.build(wb, companyId, start, end);
                case EXPENSE                -> expenseBuilder.build(wb, companyId, start, end);
                case CASH_FLOW              -> cashFlowBuilder.build(wb, companyId, start, end);
                case AR_AGING               -> arAgingBuilder.build(wb, companyId);
                case VAT_PREP               -> vatPrepBuilder.build(wb, companyId, start, end);
                case STOCK_STATUS           -> buildStockStatus(wb, companyId);
                case COLLECTION_PERFORMANCE -> collectionPerformanceBuilder.build(wb, companyId, start, end);
                case SLOW_INVENTORY        -> slowInventoryBuilder.build(wb, companyId);
                case BUDGET_VARIANCE    -> buildBudgetVariance(wb, companyId, start, end);
                case BANK_RECONCILIATION -> buildBankReconciliation(wb, companyId, start, end);
                case EXECUTIVE_SUMMARY  -> buildExecutiveSummary(wb, companyId, start, end);
                case TRIAL_BALANCE      -> buildTrialBalance(wb, start, end);
                case INCOME_STATEMENT   -> buildIncomeStatement(wb, start, end);
                case BALANCE_SHEET      -> buildBalanceSheet(wb, start, end);
                case JOURNAL_LISTING    -> buildJournalListing(wb, start, end);
            }
            wb.write(out);
        }
    }

    // BUDGET VARIANCE (Bütçe-Gerçekleşen Sapma)

    private void buildBudgetVariance(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
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


    // BANK RECONCILIATION (Banka/Kasa Mutabakat)

    private void buildBankReconciliation(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
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


    // EXECUTIVE SUMMARY (Yönetici Finans Sağlığı Özeti)

    @SuppressWarnings("null")
    private void buildExecutiveSummary(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();

        // Karlılık
        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);
        BigDecimal totalIncome = ExcelAggregationUtils.sumInvoices(paidSales).add(ExcelAggregationUtils.sumTransactions(incomes));
        BigDecimal totalExpense = ExcelAggregationUtils.sumInvoices(paidPurchases).add(ExcelAggregationUtils.sumTransactions(expenses));
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.signum() > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Tahsilat
        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue, InvoiceType.sale, companyId));

        BigDecimal arTotal = BigDecimal.ZERO;
        BigDecimal ar90Plus = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        Map<Long, BigDecimal> openByCustomer = new HashMap<>();
        Map<Long, Long> maxAgingByCustomer = new HashMap<>();

        for (Invoice inv : openInvoices) {
            BigDecimal amt = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            arTotal = arTotal.add(amt);
            long days = inv.getDueDate() != null ? java.time.temporal.ChronoUnit.DAYS.between(inv.getDueDate(), today) : 0;
            if (days > 0) overdueAmt = overdueAmt.add(amt);
            if (days > 90) ar90Plus = ar90Plus.add(amt);
            if (inv.getCustomerId() != null) {
                openByCustomer.merge(inv.getCustomerId(), amt, BigDecimal::add);
                maxAgingByCustomer.merge(inv.getCustomerId(), days, (a, b) -> Math.max(a, b));
            }
        }
        BigDecimal overduePct = arTotal.signum() > 0
                ? overdueAmt.multiply(BigDecimal.valueOf(100)).divide(arTotal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Stok
        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();
        List<Object[]> salesRows = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
        Map<Integer, BigDecimal> soldQtyMap = new HashMap<>();
        BigDecimal totalSold12M = BigDecimal.ZERO;
        for (Object[] row : salesRows) {
            BigDecimal qty = BigDecimal.valueOf(((Number) row[1]).longValue());
            soldQtyMap.put((Integer) row[0], qty);
            totalSold12M = totalSold12M.add(qty);
        }
        BigDecimal boundCapital = BigDecimal.ZERO;
        long slowMoving = 0;
        List<Object[]> riskProducts = new ArrayList<>();
        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;
            int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal bc = cost.multiply(BigDecimal.valueOf(qty));
            boundCapital = boundCapital.add(bc);
            boolean isSlow = !soldQtyMap.containsKey(stock.getProductId())
                    || soldQtyMap.get(stock.getProductId()).compareTo(BigDecimal.valueOf(1)) < 0;
            if (isSlow) {
                slowMoving++;
                if (qty > 0) riskProducts.add(new Object[]{product.getName(), bc, qty});
            }
        }
        BigDecimal avgStockTotal = stocks.stream()
                .map(s -> BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgTurnover = avgStockTotal.signum() > 0
                ? totalSold12M.divide(avgStockTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        // Özet sheet
        Sheet summary = wb.createSheet("Yönetici Özeti");
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 1, "Rapor Tarihi:", today.toString());

        Row h1 = summary.createRow(3); Cell hc1 = h1.createCell(0); hc1.setCellValue("KARLILıK"); hc1.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 4, "Toplam Gelir", totalIncome.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Toplam Gider", totalExpense.toPlainString());
        ExcelStyleUtils.writeRow(summary, 6, "Net Kar", netProfit.toPlainString());
        ExcelStyleUtils.writeRow(summary, 7, "Kar Marjı %", margin.toPlainString());

        Row h2 = summary.createRow(9); Cell hc2 = h2.createCell(0); hc2.setCellValue("TAHSİLAT"); hc2.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 10, "Toplam Açık AR", arTotal.toPlainString());
        ExcelStyleUtils.writeRow(summary, 11, "Vadesi Geçmiş %", overduePct.toPlainString());
        ExcelStyleUtils.writeRow(summary, 12, "90+ Risk Tutarı", ar90Plus.toPlainString());

        Row h3 = summary.createRow(14); Cell hc3 = h3.createCell(0); hc3.setCellValue("STOK"); hc3.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 15, "Bağlanan Para", boundCapital.toPlainString());
        ExcelStyleUtils.writeRow(summary, 16, "Yavaş Hareket Eden", String.valueOf(slowMoving));
        ExcelStyleUtils.writeRow(summary, 17, "Devir Hızı (12 ay)", avgTurnover.toPlainString());
        ExcelStyleUtils.autoSize(summary, 2);

        // Risk Müşteriler sheet
        Map<Long, String> customerNames = new HashMap<>();
        if (!openByCustomer.isEmpty()) {
            customerRepository.findAllById(openByCustomer.keySet())
                    .forEach(c -> customerNames.put(c.getCustomerId(), c.getName()));
        }
        Sheet custSheet = wb.createSheet("Risk Müşteriler");
        ExcelStyleUtils.writeHeaderRow(custSheet, 0, bold, "Müşteri", "Açık Tutar", "Maks. Gecikme (gün)");
        List<Map.Entry<Long, BigDecimal>> sortedCustomers = new ArrayList<>(openByCustomer.entrySet());
        sortedCustomers.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        int cr = 1;
        for (Map.Entry<Long, BigDecimal> e : sortedCustomers.stream().limit(10).toList()) {
            Row row = custSheet.createRow(cr++);
            row.createCell(0).setCellValue(customerNames.getOrDefault(e.getKey(), "Müşteri #" + e.getKey()));
            row.createCell(1).setCellValue(e.getValue().doubleValue());
            row.createCell(2).setCellValue(maxAgingByCustomer.getOrDefault(e.getKey(), 0L));
        }
        ExcelStyleUtils.autoSize(custSheet, 3);

        // Risk Ürünler sheet
        riskProducts.sort((a, b) -> ((BigDecimal) b[1]).compareTo((BigDecimal) a[1]));
        Sheet prodSheet = wb.createSheet("Risk Ürünler");
        ExcelStyleUtils.writeHeaderRow(prodSheet, 0, bold, "Ürün", "Bağlanan Para", "Stok Adedi");
        int pr = 1;
        for (Object[] item : riskProducts.stream().limit(10).toList()) {
            Row row = prodSheet.createRow(pr++);
            row.createCell(0).setCellValue(item[0] != null ? (String) item[0] : "");
            row.createCell(1).setCellValue(((BigDecimal) item[1]).doubleValue());
            row.createCell(2).setCellValue((int) item[2]);
        }
        ExcelStyleUtils.autoSize(prodSheet, 3);
    }


    // MİZAN (Trial Balance)

    private void buildTrialBalance(Workbook wb, LocalDate start, LocalDate end) {
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


    // GELİR TABLOSU (Income Statement)

    private void buildIncomeStatement(Workbook wb, LocalDate start, LocalDate end) {
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


    // BİLANÇO (Balance Sheet)

    private void buildBalanceSheet(Workbook wb, LocalDate start, LocalDate end) {
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


    // YEVMİYE DÖKÜMü (Journal Listing)

    private void buildJournalListing(Workbook wb, LocalDate start, LocalDate end) {
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


    // STOCK STATUS (Stok Durum)

    private void buildStockStatus(Workbook wb, Long companyId) {
        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);

        Sheet sheet = wb.createSheet("Stok Durum");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeHeaderRow(sheet, 0, bold,
                "Ürün Adı", "Barkod", "Birim", "Mevcut Stok", "Min Stok",
                "Durum", "Satış Fiyatı", "Maliyet", "Stok Değeri");

        BigDecimal grandTotal = BigDecimal.ZERO;
        int r = 1;
        for (Stock s : stocks) {
            Product p = productMap.get(s.getProductId());
            int qty = s.getQuantity() != null ? s.getQuantity() : 0;
            int minQty = s.getMinQuantity() != null ? s.getMinQuantity() : 0;
            BigDecimal sale = p != null && p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO;
            BigDecimal cost = p != null && p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO;
            BigDecimal value = sale.multiply(BigDecimal.valueOf(qty));
            grandTotal = grandTotal.add(value);
            String status = (s.getMinQuantity() != null && qty < minQty) ? "Düşük" : "Normal";

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getName()) : "Ürün #" + s.getProductId());
            row.createCell(1).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getBarcode()) : "");
            row.createCell(2).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getUnit()) : "");
            row.createCell(3).setCellValue(qty);
            row.createCell(4).setCellValue(minQty);
            row.createCell(5).setCellValue(status);
            row.createCell(6).setCellValue(sale.doubleValue());
            row.createCell(7).setCellValue(cost.doubleValue());
            row.createCell(8).setCellValue(value.doubleValue());
        }

        Row totalRow = sheet.createRow(r + 1);
        Cell tc0 = totalRow.createCell(0); tc0.setCellValue("Toplam Stok Değeri"); tc0.setCellStyle(bold);
        Cell tc1 = totalRow.createCell(8); tc1.setCellValue(grandTotal.doubleValue()); tc1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 9);
    }



}
