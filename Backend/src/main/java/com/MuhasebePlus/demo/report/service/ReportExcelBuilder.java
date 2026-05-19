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

    public void build(ReportType type, Long companyId, LocalDate start, LocalDate end, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (type) {
                case PROFIT_LOSS            -> buildProfitLoss(wb, companyId, start, end);
                case INCOME                 -> buildIncome(wb, companyId, start, end);
                case EXPENSE                -> buildExpense(wb, companyId, start, end);
                case CASH_FLOW              -> buildCashFlow(wb, companyId, start, end);
                case AR_AGING               -> buildArAging(wb, companyId);
                case VAT_PREP               -> buildVatPrep(wb, companyId, start, end);
                case STOCK_STATUS           -> buildStockStatus(wb, companyId);
                case COLLECTION_PERFORMANCE -> buildCollectionPerformance(wb, companyId, start, end);
                case SLOW_INVENTORY        -> buildSlowInventory(wb, companyId);
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




    // PROFIT/LOSS

    private void buildProfitLoss(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        BigDecimal totalRevenue = ExcelAggregationUtils.sumInvoices(paidSales).add(ExcelAggregationUtils.sumTransactions(incomes));
        BigDecimal totalExpense = ExcelAggregationUtils.sumInvoices(paidPurchases).add(ExcelAggregationUtils.sumTransactions(expenses));
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Toplam Gelir", totalRevenue.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Toplam Gider", totalExpense.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0); c0.setCellValue("Net Kâr/Zarar"); c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1); c1.setCellValue(netProfit.toPlainString()); c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet incomeSheet = wb.createSheet("Gelirler");
        writeIncomeSheet(incomeSheet, paidSales, incomes, bold);

        Sheet expenseSheet = wb.createSheet("Giderler");
        writeExpenseSheet(expenseSheet, paidPurchases, expenses, bold);
    }


    // INCOME ONLY

    private void buildIncome(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);

        Sheet sheet = wb.createSheet("Gelir Raporu");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        writeIncomeSheetAt(sheet, 2, paidSales, incomes, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }


    // EXPENSE ONLY

    private void buildExpense(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);

        Sheet sheet = wb.createSheet("Gider Raporu");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(sheet, 0, "Tarih Aralığı:", start + " - " + end);
        writeExpenseSheetAt(sheet, 2, paidPurchases, expenses, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }


    // CASH FLOW (Nakit Akış)

    private void buildCashFlow(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);
        BigDecimal inflow = ExcelAggregationUtils.sumTransactions(incomes);
        BigDecimal outflow = ExcelAggregationUtils.sumTransactions(expenses);
        BigDecimal net = inflow.subtract(outflow);

        // Özet
        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Toplam Giriş", inflow.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Toplam Çıkış", outflow.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0); c0.setCellValue("Net Nakit Akışı"); c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1); c1.setCellValue(net.toPlainString()); c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        // İşlemler — tarih sırasına göre kümülatif bakiye ile
        Sheet sheet = wb.createSheet("İşlemler");
        ExcelStyleUtils.writeHeaderRow(sheet, 0, bold, "Tarih", "Hesap ID", "Tip", "Açıklama", "Giriş", "Çıkış", "Kümülatif Bakiye");
        List<Transaction> all = new ArrayList<>();
        all.addAll(incomes);
        all.addAll(expenses);
        all.sort((a, b) -> {
            LocalDate da = a.getTransactionDate();
            LocalDate db = b.getTransactionDate();
            if (da == null && db == null) return 0;
            if (da == null) return -1;
            if (db == null) return 1;
            return da.compareTo(db);
        });
        BigDecimal running = BigDecimal.ZERO;
        int r = 1;
        for (Transaction tx : all) {
            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            boolean isIn = tx.getTransactionType() == TransactionType.INCOME;
            running = isIn ? running.add(amt) : running.subtract(amt);
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(isIn ? "GİRİŞ" : "ÇIKIŞ");
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(isIn ? amt.doubleValue() : 0);
            row.createCell(5).setCellValue(!isIn ? amt.doubleValue() : 0);
            row.createCell(6).setCellValue(running.doubleValue());
        }
        ExcelStyleUtils.autoSize(sheet, 7);
    }


    // AR AGING (Cari Yaşlandırma)

    @SuppressWarnings("null")
    private void buildArAging(Workbook wb, Long companyId) {
        LocalDate today = LocalDate.now();
        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        // Müşteri bazlı bucket
        Map<Long, BigDecimal[]> perCustomer = new HashMap<>();
        for (Invoice inv : openInvoices) {
            if (inv.getCustomerId() == null) continue;
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            long days = inv.getDueDate() != null
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                    : 0;
            BigDecimal[] arr = perCustomer.computeIfAbsent(inv.getCustomerId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
            arr[idx] = arr[idx].add(amount);
        }

        Map<Long, String> nameMap = new HashMap<>();
        if (!perCustomer.isEmpty()) {
            customerRepository.findAllById(perCustomer.keySet())
                    .forEach(c -> nameMap.put(c.getCustomerId(), c.getName()));
        }

        BigDecimal t1 = BigDecimal.ZERO, t2 = BigDecimal.ZERO, t3 = BigDecimal.ZERO, t4 = BigDecimal.ZERO;
        for (BigDecimal[] arr : perCustomer.values()) {
            t1 = t1.add(arr[0]); t2 = t2.add(arr[1]); t3 = t3.add(arr[2]); t4 = t4.add(arr[3]);
        }

        // Özet
        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih:", today.toString());
        ExcelStyleUtils.writeRow(summary, 2, "0-30 gün", t1.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "31-60 gün", t2.toPlainString());
        ExcelStyleUtils.writeRow(summary, 4, "61-90 gün", t3.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "90+ gün", t4.toPlainString());
        Row tot = summary.createRow(7);
        Cell tc0 = tot.createCell(0); tc0.setCellValue("Toplam Açık Alacak"); tc0.setCellStyle(bold);
        Cell tc1 = tot.createCell(1); tc1.setCellValue(t1.add(t2).add(t3).add(t4).toPlainString()); tc1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        // Detay
        Sheet detail = wb.createSheet("Detay");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold, "Müşteri", "0-30", "31-60", "61-90", "90+", "Toplam");
        int r = 1;
        for (Map.Entry<Long, BigDecimal[]> e : perCustomer.entrySet()) {
            BigDecimal[] arr = e.getValue();
            BigDecimal sum = arr[0].add(arr[1]).add(arr[2]).add(arr[3]);
            Row row = detail.createRow(r++);
            row.createCell(0).setCellValue(nameMap.getOrDefault(e.getKey(), "Müşteri #" + e.getKey()));
            row.createCell(1).setCellValue(arr[0].doubleValue());
            row.createCell(2).setCellValue(arr[1].doubleValue());
            row.createCell(3).setCellValue(arr[2].doubleValue());
            row.createCell(4).setCellValue(arr[3].doubleValue());
            row.createCell(5).setCellValue(sum.doubleValue());
        }
        ExcelStyleUtils.autoSize(detail, 6);
    }


    // VAT PREP (KDV Beyanname Hazırlık)

    private void buildVatPrep(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        List<Invoice> sales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> purchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        BigDecimal collected = ExcelAggregationUtils.sumInvoiceVat(sales);
        BigDecimal paid = ExcelAggregationUtils.sumInvoiceVat(purchases);
        BigDecimal net = collected.subtract(paid);

        // Özet
        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 2, "Tahsil Edilen KDV (Satış)", collected.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Ödenen KDV (Alış)", paid.toPlainString());
        Row netRow = summary.createRow(5);
        Cell c0 = netRow.createCell(0);
        c0.setCellValue(net.signum() >= 0 ? "Ödenecek KDV" : "İade Edilecek KDV");
        c0.setCellStyle(bold);
        Cell c1 = netRow.createCell(1);
        c1.setCellValue(net.abs().toPlainString());
        c1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(summary, 2);

        // Satış faturaları
        Sheet salesSheet = wb.createSheet("Satış KDV");
        ExcelStyleUtils.writeHeaderRow(salesSheet, 0, bold, "Fatura No", "Müşteri ID", "Tarih", "Matrah", "KDV", "Toplam");
        int r = 1;
        for (Invoice inv : sales) {
            Row row = salesSheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate().toString() : "");
            row.createCell(3).setCellValue(inv.getSubtotal() != null ? inv.getSubtotal().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getVatAmount() != null ? inv.getVatAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
        }
        ExcelStyleUtils.autoSize(salesSheet, 6);

        // Alış faturaları
        Sheet purchaseSheet = wb.createSheet("Alış KDV");
        ExcelStyleUtils.writeHeaderRow(purchaseSheet, 0, bold, "Fatura No", "Tedarikçi ID", "Tarih", "Matrah", "KDV", "Toplam");
        r = 1;
        for (Invoice inv : purchases) {
            Row row = purchaseSheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalDate().toString() : "");
            row.createCell(3).setCellValue(inv.getSubtotal() != null ? inv.getSubtotal().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getVatAmount() != null ? inv.getVatAmount().doubleValue() : 0);
            row.createCell(5).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
        }
        ExcelStyleUtils.autoSize(purchaseSheet, 6);
    }


    // COLLECTION PERFORMANCE (Tahsilat Performansı)

    private void buildCollectionPerformance(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);

        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        // Müşteri bazlı agregasyon
        Map<Long, BigDecimal> openByCustomer = new HashMap<>();
        Map<Long, BigDecimal> over90ByCustomer = new HashMap<>();
        Map<Long, Long> totalDelayDays = new HashMap<>();
        Map<Long, Integer> invoiceCountByCustomer = new HashMap<>();

        BigDecimal b1 = BigDecimal.ZERO, b2 = BigDecimal.ZERO, b3 = BigDecimal.ZERO, b4 = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (Invoice inv : openInvoices) {
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            long days = inv.getDueDate() != null
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                    : 0;
            if (days > 0) overdueAmount = overdueAmount.add(amount);
            if (days <= 30) b1 = b1.add(amount);
            else if (days <= 60) b2 = b2.add(amount);
            else if (days <= 90) b3 = b3.add(amount);
            else b4 = b4.add(amount);

            Long cid = inv.getCustomerId();
            if (cid == null) continue;
            openByCustomer.merge(cid, amount, BigDecimal::add);
            if (days > 90) over90ByCustomer.merge(cid, amount, BigDecimal::add);
            totalDelayDays.merge(cid, Math.max(0, days), (a, b) -> a + b);
            invoiceCountByCustomer.merge(cid, 1, (a, b) -> a + b);
        }

        BigDecimal endingAR = b1.add(b2).add(b3).add(b4);
        BigDecimal currentAR = b1;

        BigDecimal creditSales = ExcelAggregationUtils.sumInvoices(invoiceRepository
                .findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().toLocalDate().isBefore(start)
                        && !i.getCreatedAt().toLocalDate().isAfter(end))
                .toList());

        LocalDateTime startDt = start.atStartOfDay();
        BigDecimal beginningAR = ExcelAggregationUtils.sumInvoices(openInvoices.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isBefore(startDt))
                .toList());

        BigDecimal avgAR = beginningAR.add(endingAR).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal dso = creditSales.signum() > 0
                ? avgAR.multiply(BigDecimal.valueOf(periodDays)).divide(creditSales, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal arTurnover = avgAR.signum() > 0
                ? creditSales.divide(avgAR, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal overduePct = endingAR.signum() > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100)).divide(endingAR, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal numerator = beginningAR.add(creditSales).subtract(endingAR);
        BigDecimal denominator = beginningAR.add(creditSales).subtract(currentAR);
        BigDecimal cei = denominator.signum() > 0
                ? numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Sheet 1: Özet
        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 1, "Rapor Tarihi:", today.toString());
        ExcelStyleUtils.writeRow(summary, 3, "DSO (gün)", dso.toPlainString());
        ExcelStyleUtils.writeRow(summary, 4, "A/R Turnover", arTurnover.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Overdue %", overduePct.toPlainString());
        ExcelStyleUtils.writeRow(summary, 6, "CEI", cei.toPlainString());
        ExcelStyleUtils.writeRow(summary, 8, "Açık AR (toplam)", endingAR.toPlainString());
        ExcelStyleUtils.writeRow(summary, 9, "Vadesi Geçmiş Tutar", overdueAmount.toPlainString());
        ExcelStyleUtils.writeRow(summary, 10, "Dönem Kredili Satış", creditSales.toPlainString());

        // Aging dagilim
        ExcelStyleUtils.writeHeaderRow(summary, 12, bold, "Bucket", "Tutar");
        Row r1 = summary.createRow(13); r1.createCell(0).setCellValue("0-30 gün");  r1.createCell(1).setCellValue(b1.doubleValue());
        Row r2 = summary.createRow(14); r2.createCell(0).setCellValue("31-60 gün"); r2.createCell(1).setCellValue(b2.doubleValue());
        Row r3 = summary.createRow(15); r3.createCell(0).setCellValue("61-90 gün"); r3.createCell(1).setCellValue(b3.doubleValue());
        Row r4 = summary.createRow(16); r4.createCell(0).setCellValue("90+ gün");   r4.createCell(1).setCellValue(b4.doubleValue());
        ExcelStyleUtils.autoSize(summary, 2);

        // Sheet 2: Müşteri Performansı
        Sheet detail = wb.createSheet("Müşteri Performansı");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold,
                "Müşteri", "Açık Tutar", "90+ Tutar", "Ort. Gecikme (gün)", "Risk Skoru");
        Map<Long, String> nameMap = new HashMap<>();
        if (!openByCustomer.isEmpty()) {
            customerRepository.findAllById(openByCustomer.keySet())
                    .forEach(c -> nameMap.put(c.getCustomerId(), c.getName()));
        }
        BigDecimal endingARForRisk = endingAR.signum() > 0 ? endingAR : BigDecimal.ONE;
        int rr = 1;
        // En riskli müşteriler en üstte
        List<Map.Entry<Long, BigDecimal>> sorted = new ArrayList<>(openByCustomer.entrySet());
        sorted.sort((a, c) -> c.getValue().compareTo(a.getValue()));
        for (Map.Entry<Long, BigDecimal> e : sorted) {
            Long cid = e.getKey();
            BigDecimal open = e.getValue();
            BigDecimal over90 = over90ByCustomer.getOrDefault(cid, BigDecimal.ZERO);
            int count = invoiceCountByCustomer.getOrDefault(cid, 1);
            long avgDelay = totalDelayDays.getOrDefault(cid, 0L) / Math.max(1, count);
            // Risk skoru: müşteri açık tutarı / toplam ending AR oranına göre A/B/C
            BigDecimal share = open.multiply(BigDecimal.valueOf(100)).divide(endingARForRisk, 1, RoundingMode.HALF_UP);
            String risk = share.compareTo(BigDecimal.valueOf(20)) >= 0 ? "A (Yüksek)"
                        : share.compareTo(BigDecimal.valueOf(10)) >= 0 ? "B (Orta)"
                        : "C (Düşük)";
            Row row = detail.createRow(rr++);
            row.createCell(0).setCellValue(nameMap.getOrDefault(cid, "Müşteri #" + cid));
            row.createCell(1).setCellValue(open.doubleValue());
            row.createCell(2).setCellValue(over90.doubleValue());
            row.createCell(3).setCellValue(avgDelay);
            row.createCell(4).setCellValue(risk);
        }
        ExcelStyleUtils.autoSize(detail, 5);
    }


    // YAVAŞ DÖNEN STOK (Slow Inventory)

    private void buildSlowInventory(Workbook wb, Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();

        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);

        List<Object[]> salesData = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
        Map<Integer, BigDecimal> soldQtyMap = new HashMap<>();
        BigDecimal totalSold12Months = BigDecimal.ZERO;
        for (Object[] row : salesData) {
            Integer productId = (Integer) row[0];
            BigDecimal qty = BigDecimal.valueOf(((Number) row[1]).longValue());
            soldQtyMap.put(productId, qty);
            totalSold12Months = totalSold12Months.add(qty);
        }

        BigDecimal totalBoundCapital = BigDecimal.ZERO;
        long slowMovingCount = 0;
        long b1 = 0, b2 = 0, b3 = 0, b4 = 0;

        List<StockSlowItem> slowItems = new ArrayList<>();

        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;

            Integer qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal boundCapital = costPrice.multiply(BigDecimal.valueOf(qty));

            Optional<LocalDateTime> lastSaleOpt = invoiceLineItemRepository
                    .findLastSaleDateByProductId(stock.getProductId(), companyId);

            long daysSinceLastSale;
            if (lastSaleOpt.isPresent()) {
                daysSinceLastSale = ChronoUnit.DAYS.between(lastSaleOpt.get().toLocalDate(), today);
            } else {
                daysSinceLastSale = stock.getCreatedAt() != null
                        ? ChronoUnit.DAYS.between(stock.getCreatedAt().toLocalDate(), today)
                        : 9999;
            }

            if (daysSinceLastSale <= 60) b1++;
            else if (daysSinceLastSale <= 90) b2++;
            else if (daysSinceLastSale <= 180) b3++;
            else b4++;

            if (daysSinceLastSale > 90) {
                slowMovingCount++;
            }

            totalBoundCapital = totalBoundCapital.add(boundCapital);

            BigDecimal sold12Months = soldQtyMap.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
            BigDecimal avgStock = BigDecimal.valueOf(qty);
            BigDecimal turnover = avgStock.signum() > 0
                    ? sold12Months.divide(avgStock, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String status = daysSinceLastSale <= 60 ? "Aktif"
                    : daysSinceLastSale <= 90 ? "Dikkat"
                    : daysSinceLastSale <= 180 ? "Yavaş"
                    : "Risksiz";

            slowItems.add(new StockSlowItem(
                    product.getName(),
                    product.getBarcode(),
                    qty,
                    lastSaleOpt.map(LocalDateTime::toLocalDate).orElse(null),
                    daysSinceLastSale,
                    costPrice,
                    boundCapital,
                    turnover,
                    status
            ));
        }

        BigDecimal avgStockTotal = stocks.stream()
                .map(s -> BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgTurnover = avgStockTotal.signum() > 0
                ? totalSold12Months.divide(avgStockTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Rapor Tarihi:", today.toString());
        ExcelStyleUtils.writeRow(summary, 2, "Bağlanan Para", totalBoundCapital.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Yavaş Hareket Eden Ürün", String.valueOf(slowMovingCount));
        ExcelStyleUtils.writeRow(summary, 4, "Stok Devir Hızı (12 ay)", avgTurnover.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Toplam Ürün", String.valueOf(stocks.size()));

        ExcelStyleUtils.writeHeaderRow(summary, 7, bold, "Bucket", "Ürün Sayısı");
        Row r1 = summary.createRow(8); r1.createCell(0).setCellValue("0-60 gün");  r1.createCell(1).setCellValue(b1);
        Row r2 = summary.createRow(9); r2.createCell(0).setCellValue("61-90 gün"); r2.createCell(1).setCellValue(b2);
        Row r3 = summary.createRow(10); r3.createCell(0).setCellValue("91-180 gün"); r3.createCell(1).setCellValue(b3);
        Row r4 = summary.createRow(11); r4.createCell(0).setCellValue("180+ gün");  r4.createCell(1).setCellValue(b4);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet detail = wb.createSheet("Yavaş Stok Detayı");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold,
                "Ürün", "SKU", "Mevcut Stok", "Son Satış", "Geçen Gün",
                "Maliyet", "Bağlanan Para", "Devir Hızı", "Durum");

        slowItems.sort((a, b) -> Long.compare(b.daysSinceLastSale, a.daysSinceLastSale));

        int rowNum = 1;
        for (StockSlowItem item : slowItems) {
            Row row = detail.createRow(rowNum++);
            row.createCell(0).setCellValue(item.productName);
            row.createCell(1).setCellValue(item.sku != null ? item.sku : "");
            row.createCell(2).setCellValue(item.quantity);
            row.createCell(3).setCellValue(item.lastSaleDate != null ? item.lastSaleDate.toString() : "Satış yok");
            row.createCell(4).setCellValue(item.daysSinceLastSale);
            row.createCell(5).setCellValue(item.costPrice.doubleValue());
            row.createCell(6).setCellValue(item.boundCapital.doubleValue());
            row.createCell(7).setCellValue(item.turnover.doubleValue());
            row.createCell(8).setCellValue(item.status);
        }
        ExcelStyleUtils.autoSize(detail, 9);
    }

    private record StockSlowItem(
            String productName,
            String sku,
            int quantity,
            LocalDate lastSaleDate,
            long daysSinceLastSale,
            BigDecimal costPrice,
            BigDecimal boundCapital,
            BigDecimal turnover,
            String status
    ) {}


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


    // SHEET WRITERS

    private void writeIncomeSheet(Sheet sheet, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        writeIncomeSheetAt(sheet, 0, sales, incomes, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }

    private void writeIncomeSheetAt(Sheet sheet, int startRow, List<Invoice> sales, List<Transaction> incomes, CellStyle bold) {
        int r = startRow;

        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Satış - Ödenmiş)"); t1.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Fatura No", "Müşteri ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : sales) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row salesTotal = sheet.createRow(r++);
        Cell s0 = salesTotal.createCell(0); s0.setCellValue("Toplam"); s0.setCellStyle(bold);
        Cell s1 = salesTotal.createCell(3); s1.setCellValue(ExcelAggregationUtils.sumInvoices(sales).doubleValue()); s1.setCellStyle(bold);

        r++;

        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (INCOME)"); t2.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : incomes) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(ExcelAggregationUtils.sumTransactions(incomes).doubleValue()); tt1.setCellStyle(bold);
    }

    private void writeExpenseSheet(Sheet sheet, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        writeExpenseSheetAt(sheet, 0, purchases, expenses, bold);
        ExcelStyleUtils.autoSize(sheet, 5);
    }

    private void writeExpenseSheetAt(Sheet sheet, int startRow, List<Invoice> purchases, List<Transaction> expenses, CellStyle bold) {
        int r = startRow;

        Row title1 = sheet.createRow(r++);
        Cell t1 = title1.createCell(0); t1.setCellValue("FATURALAR (Alış - Ödenmiş)"); t1.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Fatura No", "Tedarikçi ID", "Vade Tarihi", "Tutar", "Durum");
        for (Invoice inv : purchases) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(ExcelStyleUtils.safeStr(inv.getInvoiceNumber()));
            row.createCell(1).setCellValue(inv.getCustomerId() != null ? inv.getCustomerId() : 0);
            row.createCell(2).setCellValue(inv.getDueDate() != null ? inv.getDueDate().toString() : "");
            row.createCell(3).setCellValue(inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
        }
        Row purchaseTotal = sheet.createRow(r++);
        Cell p0 = purchaseTotal.createCell(0); p0.setCellValue("Toplam"); p0.setCellStyle(bold);
        Cell p1 = purchaseTotal.createCell(3); p1.setCellValue(ExcelAggregationUtils.sumInvoices(purchases).doubleValue()); p1.setCellStyle(bold);

        r++;

        Row title2 = sheet.createRow(r++);
        Cell t2 = title2.createCell(0); t2.setCellValue("İŞLEMLER (EXPENSE)"); t2.setCellStyle(bold);
        ExcelStyleUtils.writeHeaderRow(sheet, r++, bold, "Tarih", "Hesap ID", "Tutar", "Açıklama", "Kategori");
        for (Transaction tx : expenses) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
            row.createCell(1).setCellValue(tx.getAccountId() != null ? tx.getAccountId() : 0);
            row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(ExcelStyleUtils.safeStr(tx.getDescription()));
            row.createCell(4).setCellValue(ExcelStyleUtils.safeStr(tx.getCategory()));
        }
        Row txTotal = sheet.createRow(r++);
        Cell tt0 = txTotal.createCell(0); tt0.setCellValue("Toplam"); tt0.setCellStyle(bold);
        Cell tt1 = txTotal.createCell(2); tt1.setCellValue(ExcelAggregationUtils.sumTransactions(expenses).doubleValue()); tt1.setCellStyle(bold);
    }


}
