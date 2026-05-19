
package com.MuhasebePlus.demo.report.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
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
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.dto.request.ReportPreviewRequestDto;
import com.MuhasebePlus.demo.report.dto.request.ReportRequestDto;
import com.MuhasebePlus.demo.report.dto.response.BucketPoint;
import com.MuhasebePlus.demo.report.dto.response.KpiItem;
import com.MuhasebePlus.demo.report.dto.response.MonthlyPoint;
import com.MuhasebePlus.demo.report.dto.response.PreviewSection;
import com.MuhasebePlus.demo.report.dto.response.ReportPreviewResponseDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.repository.ReportRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;

import java.util.LinkedHashMap;

@Service
@Transactional
public class ReportService implements HardDeletable {

    private final ReportRepository reportRepository;
    private final ReportExcelBuilder excelBuilder;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CustomerRepository customerRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final BudgetRepository budgetRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalEntryService journalEntryService;
    private final String storagePath;

    public ReportService(
            ReportRepository reportRepository,
            ReportExcelBuilder excelBuilder,
            CompanyContext companyContext,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            InvoiceRepository invoiceRepository,
            InvoiceLineItemRepository invoiceLineItemRepository,
            CustomerRepository customerRepository,
            StockRepository stockRepository,
            ProductRepository productRepository,
            BudgetRepository budgetRepository,
            BankAccountRepository bankAccountRepository,
            JournalEntryService journalEntryService,
            @Value("${app.report.storage-path:./reports/}") String storagePath) {
        this.reportRepository = reportRepository;
        this.excelBuilder = excelBuilder;
        this.companyContext = companyContext;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.customerRepository = customerRepository;
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.budgetRepository = budgetRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.journalEntryService = journalEntryService;
        this.storagePath = storagePath;
    }


    // PUBLIC METOTLAR

    public ReportResponseDto generateReport(ReportRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        if (dto.startDate().isAfter(dto.endDate())) {
            throw new RuntimeException("Başlangıç tarihi bitiş tarihinden büyük olamaz");
        }

        Report report = new Report();
        report.setCompany(companyRepository.getReferenceById(companyId));
        report.setUserId(userId);
        report.setReportType(dto.reportType());
        report.setStartDate(dto.startDate());
        report.setEndDate(dto.endDate());
        report.setFormat(dto.format());
        report.setGeneratedAt(LocalDateTime.now());
        report.setDeleted(false);
        Report saved = reportRepository.save(report);

        Path filePath = buildFilePath(companyId, saved.getReportId());
        try {
            Files.createDirectories(filePath.getParent());
            try (OutputStream out = Files.newOutputStream(filePath)) {
                excelBuilder.build(dto.reportType(), companyId, dto.startDate(), dto.endDate(), out);
            }
            saved.setFilePath(filePath.toString());
            saved.setFileSize(Files.size(filePath));
            reportRepository.save(saved);
        } catch (IOException e) {
            throw new RuntimeException("Rapor dosyası oluşturulamadı: " + e.getMessage(), e);
        }

        return toResponseDto(saved);
    }

    public byte[] downloadReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = findActiveReportById(reportId, companyId);

        if (report.getFilePath() == null) {
            throw new RuntimeException("Bu raporun dosyası bulunamadı");
        }

        try {
            return Files.readAllBytes(Paths.get(report.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Rapor dosyası okunamadı: " + e.getMessage(), e);
        }
    }

    public List<ReportResponseDto> getAllReports() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Report> reports = reportRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(companyId);
        Set<Long> userIds = reports.stream()
                .map(Report::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));
        return reports.stream().map(r -> toResponseDto(r, userMap)).toList();
    }

    public ReportResponseDto getReportById(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(findActiveReportById(reportId, companyId));
    }

    public void softDeleteReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = findActiveReportById(reportId, companyId);
        report.setDeleted(true);
        report.setDeletedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    public ReportResponseDto restoreReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = reportRepository.findByReportIdAndCompanyCompanyId(reportId, companyId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        report.setDeleted(false);
        report.setDeletedAt(null);
        Report restored = reportRepository.save(report);
        return toResponseDto(restored);
    }

    public ReportPreviewResponseDto previewReport(ReportPreviewRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (dto.startDate().isAfter(dto.endDate())) {
            throw new RuntimeException("Başlangıç tarihi bitiş tarihinden büyük olamaz");
        }

        return switch (dto.reportType()) {
            case PROFIT_LOSS, INCOME, EXPENSE -> previewProfitLoss(companyId, dto.reportType(), dto.startDate(), dto.endDate());
            case CASH_FLOW -> previewCashFlow(companyId, dto.startDate(), dto.endDate());
            case AR_AGING -> previewArAging(companyId);
            case VAT_PREP -> previewVatPrep(companyId, dto.startDate(), dto.endDate());
            case STOCK_STATUS -> previewStockStatus(companyId);
            case COLLECTION_PERFORMANCE -> previewCollectionPerformance(companyId, dto.startDate(), dto.endDate());
            case SLOW_INVENTORY -> previewSlowInventory(companyId);
            case BUDGET_VARIANCE -> previewBudgetVariance(companyId, dto.startDate(), dto.endDate());
            case BANK_RECONCILIATION -> previewBankReconciliation(companyId, dto.startDate(), dto.endDate());
            case EXECUTIVE_SUMMARY -> previewExecutiveSummary(companyId, dto.startDate(), dto.endDate());
            case TRIAL_BALANCE -> previewTrialBalance(dto.startDate(), dto.endDate());
            case INCOME_STATEMENT -> previewIncomeStatement(dto.startDate(), dto.endDate());
            case BALANCE_SHEET -> previewBalanceSheet(dto.startDate(), dto.endDate());
            case JOURNAL_LISTING -> notImplementedSection(ReportType.JOURNAL_LISTING);
        };
    }

    private ReportPreviewResponseDto notImplementedSection(ReportType type) {
        return single(
                List.of(new KpiItem(type.name() + " — Yakında", BigDecimal.ZERO, "count", "neutral")),
                null, null, null, null
        );
    }

    private ReportPreviewResponseDto single(
            List<KpiItem> kpis,
            BigDecimal progressValue,
            String progressLabel,
            List<MonthlyPoint> monthlySeries,
            List<BucketPoint> buckets) {
        return new ReportPreviewResponseDto(List.of(
                new PreviewSection(null, kpis, progressValue, progressLabel, monthlySeries, buckets)
        ));
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Report> expired = reportRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Report report : expired) {
            if (report.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(report.getFilePath()));
                } catch (IOException ignored) {
                }
            }
            reportRepository.delete(report);
        }
        return expired.size();
    }


    // PREVIEW DISPATCH IMPLEMENTASYONLARI

    private ReportPreviewResponseDto previewProfitLoss(Long companyId, ReportType type, LocalDate start, LocalDate end) {
        BigDecimal totalIncome = computeIncomeTotal(companyId, type, start, end);
        BigDecimal totalExpense = computeExpenseTotal(companyId, type, start, end);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.signum() > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Gelir",  totalIncome,  "currency", "pos"),
                new KpiItem("Toplam Gider",  totalExpense, "currency", "neg"),
                new KpiItem("Net Kar",       netProfit,    "currency", "neutral")
        );
        List<MonthlyPoint> monthlySeries = buildMonthlySeries(companyId, type, end);
        return single(kpis, margin, "Kar Marjı", monthlySeries, null);
    }

    private ReportPreviewResponseDto previewCashFlow(Long companyId, LocalDate start, LocalDate end) {
        BigDecimal inflow = sumTransactions(fetchTransactions(companyId, TransactionType.INCOME, start, end));
        BigDecimal outflow = sumTransactions(fetchTransactions(companyId, TransactionType.EXPENSE, start, end));
        BigDecimal net = inflow.subtract(outflow);

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Giriş", inflow,  "currency", "pos"),
                new KpiItem("Toplam Çıkış", outflow, "currency", "neg"),
                new KpiItem("Net Nakit",    net,     "currency", net.signum() >= 0 ? "pos" : "neg")
        );

        // 12 aylık nakit serisi
        YearMonth endMonth = YearMonth.from(end);
        List<MonthlyPoint> monthly = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = endMonth.minusMonths(i);
            LocalDate s = ym.atDay(1);
            LocalDate e = ym.atEndOfMonth();
            BigDecimal in = sumTransactions(fetchTransactions(companyId, TransactionType.INCOME, s, e));
            BigDecimal out = sumTransactions(fetchTransactions(companyId, TransactionType.EXPENSE, s, e));
            monthly.add(new MonthlyPoint(monthLabel(ym), in, out));
        }
        return single(kpis, null, null, monthly, null);
    }

    private ReportPreviewResponseDto previewArAging(Long companyId) {
        LocalDate today = LocalDate.now();
        List<Invoice> openInvoices = invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId);
        // overdue olanları da ekle
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        BigDecimal b1 = BigDecimal.ZERO; // 0-30
        BigDecimal b2 = BigDecimal.ZERO; // 31-60
        BigDecimal b3 = BigDecimal.ZERO; // 61-90
        BigDecimal b4 = BigDecimal.ZERO; // 90+
        java.util.Set<Long> customers = new java.util.HashSet<>();

        for (Invoice inv : openInvoices) {
            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            if (inv.getCustomerId() != null) customers.add(inv.getCustomerId());
            long days = inv.getDueDate() != null
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                    : 0;
            if (days <= 30) b1 = b1.add(amount);
            else if (days <= 60) b2 = b2.add(amount);
            else if (days <= 90) b3 = b3.add(amount);
            else b4 = b4.add(amount);
        }

        BigDecimal total = b1.add(b2).add(b3).add(b4);

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Açık Alacak", total, "currency", "neutral"),
                new KpiItem("Müşteri Sayısı",     BigDecimal.valueOf(customers.size()), "count", "neutral"),
                new KpiItem("90+ Gün Risk",       b4,    "currency", "neg")
        );
        List<BucketPoint> buckets = List.of(
                new BucketPoint("0-30 gün",  b1),
                new BucketPoint("31-60 gün", b2),
                new BucketPoint("61-90 gün", b3),
                new BucketPoint("90+ gün",   b4)
        );
        return single(kpis, null, null, null, buckets);
    }

    private ReportPreviewResponseDto previewVatPrep(Long companyId, LocalDate start, LocalDate end) {
        BigDecimal collected = sumInvoiceVat(fetchPaidInvoices(companyId, InvoiceType.sale, start, end));
        BigDecimal paid = sumInvoiceVat(fetchPaidInvoices(companyId, InvoiceType.purchase, start, end));
        BigDecimal net = collected.subtract(paid);

        String netLabel = net.signum() >= 0 ? "Ödenecek KDV" : "İade Edilecek KDV";
        BigDecimal netDisplay = net.abs();

        List<KpiItem> kpis = List.of(
                new KpiItem("Tahsil Edilen KDV", collected, "currency", "pos"),
                new KpiItem("Ödenen KDV",        paid,      "currency", "neg"),
                new KpiItem(netLabel,            netDisplay, "currency", net.signum() >= 0 ? "neutral" : "pos")
        );
        List<BucketPoint> buckets = List.of(
                new BucketPoint("Tahsil Edilen", collected),
                new BucketPoint("Ödenen", paid)
        );
        return single(kpis, null, null, null, buckets);
    }

    private ReportPreviewResponseDto previewStockStatus(Long companyId) {
        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = loadProductMap(companyId, stocks);

        long totalProducts = stocks.size();
        long lowStock = stocks.stream()
                .filter(s -> s.getMinQuantity() != null && s.getQuantity() != null && s.getQuantity() < s.getMinQuantity())
                .count();
        BigDecimal totalValue = stocks.stream()
                .map(s -> {
                    Product p = productMap.get(s.getProductId());
                    BigDecimal price = p != null && p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO;
                    int qty = s.getQuantity() != null ? s.getQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Ürün",       BigDecimal.valueOf(totalProducts), "count",    "neutral"),
                new KpiItem("Düşük Stok",        BigDecimal.valueOf(lowStock),      "count",    "neg"),
                new KpiItem("Toplam Stok Değeri", totalValue,                       "currency", "pos")
        );
        List<BucketPoint> buckets = List.of(
                new BucketPoint("Düşük", BigDecimal.valueOf(lowStock)),
                new BucketPoint("Normal", BigDecimal.valueOf(Math.max(0, totalProducts - lowStock)))
        );
        return single(kpis, null, null, null, buckets);
    }


    // TAHSILAT PERFORMANS (Collection Performance)

    private ReportPreviewResponseDto previewCollectionPerformance(Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        long periodDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);

        // Tüm açık satış faturalari (pending + overdue)
        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        // Aging bucket'lari
        BigDecimal b1 = BigDecimal.ZERO; // 0-30
        BigDecimal b2 = BigDecimal.ZERO; // 31-60
        BigDecimal b3 = BigDecimal.ZERO; // 61-90
        BigDecimal b4 = BigDecimal.ZERO; // 90+
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
        }

        BigDecimal endingAR = b1.add(b2).add(b3).add(b4);
        BigDecimal currentAR = b1; // 0-30 = vadesi gecmemis

        // Donem icindeki kredili satislar (tum sale faturalari, kredili kabul ediyoruz)
        BigDecimal creditSales = sumInvoices(invoiceRepository
                .findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().toLocalDate().isBefore(start)
                        && !i.getCreatedAt().toLocalDate().isAfter(end))
                .toList());

        // Donem basindaki AR — donemden once olusturulmus, henuz odenmemis
        LocalDateTime startDt = start.atStartOfDay();
        BigDecimal beginningAR = sumInvoices(openInvoices.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isBefore(startDt))
                .toList());

        BigDecimal avgAR = beginningAR.add(endingAR).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        // DSO
        BigDecimal dso = creditSales.signum() > 0
                ? avgAR.multiply(BigDecimal.valueOf(periodDays))
                       .divide(creditSales, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // A/R Turnover
        BigDecimal arTurnover = avgAR.signum() > 0
                ? creditSales.divide(avgAR, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Overdue %
        BigDecimal overduePct = endingAR.signum() > 0
                ? overdueAmount.multiply(BigDecimal.valueOf(100)).divide(endingAR, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // CEI = ((BeginAR + Sales - EndTotal) / (BeginAR + Sales - EndCurrent)) × 100
        BigDecimal numerator = beginningAR.add(creditSales).subtract(endingAR);
        BigDecimal denominator = beginningAR.add(creditSales).subtract(currentAR);
        BigDecimal cei = denominator.signum() > 0
                ? numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> kpis = List.of(
                new KpiItem("DSO (gün)",        dso,        "count",    "neutral"),
                new KpiItem("A/R Turnover",     arTurnover, "count",    "pos"),
                new KpiItem("Overdue %",        overduePct, "count",    overduePct.compareTo(BigDecimal.valueOf(25)) > 0 ? "neg" : "pos"),
                new KpiItem("CEI",              cei,        "count",    cei.compareTo(BigDecimal.valueOf(80)) >= 0 ? "pos" : "neg")
        );
        List<BucketPoint> buckets = List.of(
                new BucketPoint("0-30 gün",  b1),
                new BucketPoint("31-60 gün", b2),
                new BucketPoint("61-90 gün", b3),
                new BucketPoint("90+ gün",   b4)
        );
        return single(kpis, overduePct, "Overdue %", null, buckets);
    }


    // YAVAŞ DÖNEN STOK (Slow Inventory)

    private ReportPreviewResponseDto previewSlowInventory(Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();

        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = loadProductMap(companyId, stocks);

        List<Integer> productIds = stocks.stream().map(Stock::getProductId).toList();

        Map<Integer, LocalDateTime> lastSaleDateMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : invoiceLineItemRepository.findLastSaleDatesByProductIds(companyId, productIds)) {
                lastSaleDateMap.put((Integer) row[0], (LocalDateTime) row[1]);
            }
        }

        Map<Integer, BigDecimal> soldQty12MonthsMap = new HashMap<>();
        for (Object[] row : invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo)) {
            soldQty12MonthsMap.put((Integer) row[0], BigDecimal.valueOf(((Number) row[1]).longValue()));
        }

        BigDecimal totalBoundCapital = BigDecimal.ZERO;
        long slowMovingCount = 0;
        BigDecimal totalSoldQty12Months = BigDecimal.ZERO;
        int productsWithSales = 0;

        long b1 = 0, b2 = 0, b3 = 0, b4 = 0;

        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;

            Integer qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal boundCapital = costPrice.multiply(BigDecimal.valueOf(qty));

            LocalDateTime lastSaleDate = lastSaleDateMap.get(stock.getProductId());
            long daysSinceLastSale;
            if (lastSaleDate != null) {
                daysSinceLastSale = ChronoUnit.DAYS.between(lastSaleDate.toLocalDate(), today);
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

            BigDecimal soldQty = soldQty12MonthsMap.get(stock.getProductId());
            if (soldQty != null) {
                totalSoldQty12Months = totalSoldQty12Months.add(soldQty);
                productsWithSales++;
            }
        }

        BigDecimal avgStock = stocks.isEmpty() ? BigDecimal.ZERO
                : totalSoldQty12Months.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal avgDailySales = avgStock.signum() > 0
                ? totalSoldQty12Months.divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgDaysOnHand = avgDailySales.signum() > 0
                ? avgStock.divide(avgDailySales, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgTurnover = avgStock.signum() > 0
                ? totalSoldQty12Months.divide(avgStock, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> kpis = List.of(
                new KpiItem("Bağlanan Para",    totalBoundCapital,             "currency", "neutral"),
                new KpiItem("Yavaş Hareket Eden", BigDecimal.valueOf(slowMovingCount), "count",    "neg"),
                new KpiItem("Stok Devir Hızı",   avgTurnover,                  "count",    avgTurnover.compareTo(BigDecimal.valueOf(4)) >= 0 ? "pos" : "neg")
        );
        List<BucketPoint> buckets = List.of(
                new BucketPoint("0-60 gün",  BigDecimal.valueOf(b1)),
                new BucketPoint("61-90 gün", BigDecimal.valueOf(b2)),
                new BucketPoint("91-180 gün", BigDecimal.valueOf(b3)),
                new BucketPoint("180+ gün",  BigDecimal.valueOf(b4))
        );
        return single(kpis, null, null, null, buckets);
    }


    // BÜTÇE-GERÇEKLEŞEN SAPMA (Budget Variance)

    private ReportPreviewResponseDto previewBudgetVariance(Long companyId, LocalDate start, LocalDate end) {
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

        java.util.Map<String, BigDecimal> plannedByCategory = new java.util.HashMap<>();
        for (Budget b : periodBudgets) {
            String cat = b.getCategory() != null ? b.getCategory() : "Diğer";
            plannedByCategory.merge(cat, b.getPlannedAmount() != null ? b.getPlannedAmount() : BigDecimal.ZERO, BigDecimal::add);
        }
        java.util.Map<String, BigDecimal> actualByCategory = new java.util.HashMap<>();
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

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Plan", totalPlanned, "currency", "neutral"),
                new KpiItem("Toplam Gerçekleşen", totalActual, "currency", "neutral"),
                new KpiItem("Sapma", totalVariance, "currency", totalVariance.signum() > 0 ? "neg" : "pos"),
                new KpiItem("Sapma %", variancePct, "count", variancePct.compareTo(BigDecimal.valueOf(20)) > 0 ? "neg" : "pos")
        );

        java.util.Set<String> allCats = new java.util.LinkedHashSet<>();
        allCats.addAll(plannedByCategory.keySet());
        allCats.addAll(actualByCategory.keySet());
        List<BucketPoint> buckets = new ArrayList<>();
        for (String cat : allCats) {
            BigDecimal pl = plannedByCategory.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal ac = actualByCategory.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal var = ac.subtract(pl);
            String label = cat + " (" + (var.signum() >= 0 ? "+" : "") + var.toPlainString() + ")";
            buckets.add(new BucketPoint(label, ac));
        }
        // Sırala: en büyük gerçekleşen üstte
        buckets.sort((a, b) -> b.value().compareTo(a.value()));

        return single(kpis, variancePct, "Sapma %", null, buckets);
    }


    // BANKA/KASA MUTABAKAT (Bank Reconciliation)

    private ReportPreviewResponseDto previewBankReconciliation(Long companyId, LocalDate start, LocalDate end) {
        List<Transaction> allTx = transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(start, end, companyId);

        List<BankAccount> accounts = bankAccountRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(companyId);

        java.util.Map<Long, String> accountNames = new java.util.HashMap<>();
        for (BankAccount a : accounts) {
            accountNames.put(a.getAccountId(), a.getBankName() != null ? a.getBankName() : "Hesap #" + a.getAccountId());
        }

        java.util.Map<Long, BigDecimal> inByAccount = new java.util.HashMap<>();
        java.util.Map<Long, BigDecimal> outByAccount = new java.util.HashMap<>();
        int missingDescCount = 0;

        for (Transaction tx : allTx) {
            if (tx.getDescription() == null || tx.getDescription().isBlank()) {
                missingDescCount++;
            }
            if (tx.getAccountId() == null) continue;
            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getTransactionType() == TransactionType.INCOME) {
                inByAccount.merge(tx.getAccountId(), amt, BigDecimal::add);
            } else {
                outByAccount.merge(tx.getAccountId(), amt, BigDecimal::add);
            }
        }

        // Çift kayıt tespiti
        java.util.Map<String, List<Transaction>> dupGroups = new java.util.HashMap<>();
        for (Transaction tx : allTx) {
            String key = tx.getAccountId() + "|" + tx.getTransactionDate() + "|" + tx.getAmount() + "|" + tx.getTransactionType();
            dupGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
        }
        long duplicateCount = dupGroups.values().stream().filter(l -> l.size() > 1).count();

        BigDecimal totalNet = BigDecimal.ZERO;
        List<BucketPoint> buckets = new ArrayList<>();
        for (BankAccount acc : accounts) {
            BigDecimal in = inByAccount.getOrDefault(acc.getAccountId(), BigDecimal.ZERO);
            BigDecimal out = outByAccount.getOrDefault(acc.getAccountId(), BigDecimal.ZERO);
            BigDecimal net = in.subtract(out);
            totalNet = totalNet.add(net);
            buckets.add(new BucketPoint(accountNames.get(acc.getAccountId()), net));
        }
        // Sırala: en yüksek net bakiye üstte
        buckets.sort((a, b) -> b.value().compareTo(a.value()));

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Hesap", BigDecimal.valueOf(accounts.size()), "count", "neutral"),
                new KpiItem("Net Toplam Bakiye", totalNet, "currency", totalNet.signum() >= 0 ? "pos" : "neg"),
                new KpiItem("Eksik Açıklama", BigDecimal.valueOf(missingDescCount), "count", missingDescCount > 0 ? "neg" : "pos"),
                new KpiItem("Olası Çift Kayıt", BigDecimal.valueOf(duplicateCount), "count", duplicateCount > 0 ? "neg" : "pos")
        );

        return single(kpis, null, null, null, buckets);
    }


    // YÖNETİCİ FİNANS SAĞLIĞI ÖZETİ (Executive Summary)

    private ReportPreviewResponseDto previewExecutiveSummary(Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();

        // --- Karlılık ---
        List<Invoice> paidSales = fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incTx = fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expTx = fetchTransactions(companyId, TransactionType.EXPENSE, start, end);
        BigDecimal totalIncome = sumInvoices(paidSales).add(sumTransactions(incTx));
        BigDecimal totalExpense = sumInvoices(paidPurchases).add(sumTransactions(expTx));
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.signum() > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> profitabilityKpis = List.of(
                new KpiItem("Toplam Gelir", totalIncome, "currency", "pos"),
                new KpiItem("Toplam Gider", totalExpense, "currency", "neg"),
                new KpiItem("Net Kar", netProfit, "currency", netProfit.signum() >= 0 ? "pos" : "neg")
        );
        List<MonthlyPoint> monthlySeries = buildMonthlySeries(companyId, ReportType.PROFIT_LOSS, end);

        // --- Tahsilat ---
        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, InvoiceType.sale, companyId));

        BigDecimal arTotal = BigDecimal.ZERO;
        BigDecimal ar90Plus = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        BigDecimal b1 = BigDecimal.ZERO, b2 = BigDecimal.ZERO, b3 = BigDecimal.ZERO, b4 = BigDecimal.ZERO;

        for (Invoice inv : openInvoices) {
            BigDecimal amt = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            arTotal = arTotal.add(amt);
            long days = inv.getDueDate() != null ? ChronoUnit.DAYS.between(inv.getDueDate(), today) : 0;
            if (days > 0) overdueAmt = overdueAmt.add(amt);
            if (days > 90) ar90Plus = ar90Plus.add(amt);
            if (days <= 30) b1 = b1.add(amt);
            else if (days <= 60) b2 = b2.add(amt);
            else if (days <= 90) b3 = b3.add(amt);
            else b4 = b4.add(amt);
        }
        BigDecimal overduePct = arTotal.signum() > 0
                ? overdueAmt.multiply(BigDecimal.valueOf(100)).divide(arTotal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> collectionKpis = List.of(
                new KpiItem("Toplam Açık Alacak", arTotal, "currency", "neutral"),
                new KpiItem("Vadesi Geçmiş %", overduePct, "count", overduePct.compareTo(BigDecimal.valueOf(25)) > 0 ? "neg" : "pos"),
                new KpiItem("90+ Gün Risk", ar90Plus, "currency", "neg")
        );
        List<BucketPoint> collectionBuckets = List.of(
                new BucketPoint("0-30 gün", b1),
                new BucketPoint("31-60 gün", b2),
                new BucketPoint("61-90 gün", b3),
                new BucketPoint("90+ gün", b4)
        );

        // --- Stok ---
        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = loadProductMap(companyId, stocks);
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();
        List<Object[]> salesRows = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
        Map<Integer, BigDecimal> soldQtyMap = new HashMap<>();
        BigDecimal totalSold12M = BigDecimal.ZERO;
        for (Object[] row : salesRows) {
            BigDecimal qty = BigDecimal.valueOf(((Number) row[1]).longValue());
            soldQtyMap.put((Integer) row[0], qty);
            totalSold12M = totalSold12M.add(qty);
        }

        List<Integer> stockProductIds = stocks.stream().map(Stock::getProductId).toList();
        Map<Integer, LocalDateTime> lastSaleDateMapExec = new HashMap<>();
        if (!stockProductIds.isEmpty()) {
            for (Object[] row : invoiceLineItemRepository.findLastSaleDatesByProductIds(companyId, stockProductIds)) {
                lastSaleDateMapExec.put((Integer) row[0], (LocalDateTime) row[1]);
            }
        }

        BigDecimal boundCapital = BigDecimal.ZERO;
        long slowMoving = 0;
        long sb1 = 0, sb2 = 0, sb3 = 0, sb4 = 0;

        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;
            int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal bc = cost.multiply(BigDecimal.valueOf(qty));
            boundCapital = boundCapital.add(bc);

            LocalDateTime lastSaleDate = lastSaleDateMapExec.get(stock.getProductId());
            long daysSinceLastSale;
            if (lastSaleDate != null) {
                daysSinceLastSale = ChronoUnit.DAYS.between(lastSaleDate.toLocalDate(), today);
            } else {
                daysSinceLastSale = stock.getCreatedAt() != null
                        ? ChronoUnit.DAYS.between(stock.getCreatedAt().toLocalDate(), today)
                        : 9999;
            }
            if (daysSinceLastSale <= 60) sb1++;
            else if (daysSinceLastSale <= 90) sb2++;
            else if (daysSinceLastSale <= 180) sb3++;
            else sb4++;

            if (daysSinceLastSale > 90 || !soldQtyMap.containsKey(stock.getProductId())) {
                slowMoving++;
            }
        }
        BigDecimal avgStockTotal = stocks.stream()
                .map(s -> BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgTurnover = avgStockTotal.signum() > 0
                ? totalSold12M.divide(avgStockTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> stockKpis = List.of(
                new KpiItem("Bağlanan Sermaye", boundCapital, "currency", "neutral"),
                new KpiItem("Yavaş Hareket Eden", BigDecimal.valueOf(slowMoving), "count", "neg"),
                new KpiItem("Stok Devir Hızı", avgTurnover, "count", avgTurnover.compareTo(BigDecimal.valueOf(4)) >= 0 ? "pos" : "neg")
        );
        List<BucketPoint> stockBuckets = List.of(
                new BucketPoint("0-60 gün", BigDecimal.valueOf(sb1)),
                new BucketPoint("61-90 gün", BigDecimal.valueOf(sb2)),
                new BucketPoint("91-180 gün", BigDecimal.valueOf(sb3)),
                new BucketPoint("180+ gün", BigDecimal.valueOf(sb4))
        );

        // Multi-section response
        List<PreviewSection> sections = List.of(
                new PreviewSection("Karlılık", profitabilityKpis, margin, "Kar Marjı %", monthlySeries, null),
                new PreviewSection("Tahsilat", collectionKpis, overduePct, "Vadesi Geçmiş %", null, collectionBuckets),
                new PreviewSection("Stok", stockKpis, null, null, null, stockBuckets)
        );
        return new ReportPreviewResponseDto(sections);
    }


    // MİZAN (Trial Balance)

    private ReportPreviewResponseDto previewTrialBalance(LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);
        BigDecimal totalDebit = rows.stream().map(TrialBalanceRowDto::totalDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = rows.stream().map(TrialBalanceRowDto::totalCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = totalDebit.subtract(totalCredit).abs();

        List<KpiItem> kpis = List.of(
                new KpiItem("Hesap Sayısı",   BigDecimal.valueOf(rows.size()), "count",    "neutral"),
                new KpiItem("Toplam Borç",    totalDebit,                      "currency", "neutral"),
                new KpiItem("Toplam Alacak",  totalCredit,                     "currency", "neutral"),
                new KpiItem("Fark",           difference,                      "currency", difference.signum() == 0 ? "pos" : "neg")
        );

        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (TrialBalanceRowDto row : rows) {
            String key = row.accountType() != null ? row.accountType().name() : "DİĞER";
            byType.merge(key, row.balance().abs(), BigDecimal::add);
        }
        List<BucketPoint> buckets = byType.entrySet().stream()
                .map(e -> new BucketPoint(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.value().compareTo(a.value()))
                .toList();

        BigDecimal balancePct = difference.signum() == 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return single(kpis, balancePct, "Denge %", null, buckets.isEmpty() ? null : buckets);
    }


    // GELİR TABLOSU (Income Statement)

    private ReportPreviewResponseDto previewIncomeStatement(LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);

        BigDecimal totalIncome = rows.stream()
                .filter(r -> r.accountType() == AccountType.INCOME)
                .map(r -> r.balance().negate())  // credit-normal: credit>debit → negative balance → negate to get positive revenue
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = rows.stream()
                .filter(r -> r.accountType() == AccountType.EXPENSE || r.accountType() == AccountType.COST)
                .map(TrialBalanceRowDto::balance)  // debit-normal: positive balance = expense
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.signum() > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Gelir",  totalIncome,  "currency", "pos"),
                new KpiItem("Toplam Gider",  totalExpense, "currency", "neg"),
                new KpiItem("Net Kâr/Zarar", netProfit,    "currency", netProfit.signum() >= 0 ? "pos" : "neg")
        );

        List<BucketPoint> buckets = new ArrayList<>();
        rows.stream()
                .filter(r -> r.accountType() == AccountType.INCOME)
                .forEach(r -> buckets.add(new BucketPoint(r.accountCode() + " " + r.accountName(), r.balance().negate().max(BigDecimal.ZERO))));
        rows.stream()
                .filter(r -> r.accountType() == AccountType.EXPENSE || r.accountType() == AccountType.COST)
                .forEach(r -> buckets.add(new BucketPoint(r.accountCode() + " " + r.accountName(), r.balance().max(BigDecimal.ZERO))));

        return single(kpis, margin, "Kâr Marjı %", null, buckets.isEmpty() ? null : buckets);
    }


    // BİLANÇO (Balance Sheet)

    private ReportPreviewResponseDto previewBalanceSheet(LocalDate start, LocalDate end) {
        List<TrialBalanceRowDto> rows = journalEntryService.getTrialBalance(start, end);

        BigDecimal totalAssets = rows.stream()
                .filter(r -> r.accountType() == AccountType.ASSET)
                .map(TrialBalanceRowDto::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = rows.stream()
                .filter(r -> r.accountType() == AccountType.LIABILITY)
                .map(r -> r.balance().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = rows.stream()
                .filter(r -> r.accountType() == AccountType.EQUITY)
                .map(r -> r.balance().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal passiveTotal = totalLiabilities.add(totalEquity);
        BigDecimal difference = totalAssets.subtract(passiveTotal).abs();

        List<KpiItem> kpis = List.of(
                new KpiItem("Toplam Aktif",  totalAssets,     "currency", "neutral"),
                new KpiItem("Toplam Pasif",  passiveTotal,    "currency", "neutral"),
                new KpiItem("Öz Kaynak",     totalEquity,     "currency", totalEquity.signum() >= 0 ? "pos" : "neg"),
                new KpiItem("Denge Farkı",   difference,      "currency", difference.signum() == 0 ? "pos" : "neg")
        );

        List<BucketPoint> buckets = List.of(
                new BucketPoint("Dönen Varlıklar (1xx)",  rows.stream().filter(r -> r.accountType() == AccountType.ASSET && r.accountCode().startsWith("1")).map(TrialBalanceRowDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add)),
                new BucketPoint("Duran Varlıklar (2xx)",  rows.stream().filter(r -> r.accountType() == AccountType.ASSET && r.accountCode().startsWith("2")).map(TrialBalanceRowDto::balance).reduce(BigDecimal.ZERO, BigDecimal::add)),
                new BucketPoint("K.V. Yabancı Kay. (3xx)", rows.stream().filter(r -> r.accountType() == AccountType.LIABILITY && r.accountCode().startsWith("3")).map(r -> r.balance().negate()).reduce(BigDecimal.ZERO, BigDecimal::add)),
                new BucketPoint("U.V. Yabancı Kay. (4xx)", rows.stream().filter(r -> r.accountType() == AccountType.LIABILITY && r.accountCode().startsWith("4")).map(r -> r.balance().negate()).reduce(BigDecimal.ZERO, BigDecimal::add)),
                new BucketPoint("Öz Kaynaklar (5xx)",     totalEquity)
        );

        return single(kpis, null, null, null, buckets);
    }


    // YARDIMCI METOTLAR

    private Report findActiveReportById(Long reportId, Long companyId) {
        return reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(reportId, companyId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));
    }

    private Path buildFilePath(Long companyId, Long reportId) {
        long timestamp = System.currentTimeMillis();
        String fileName = String.format("report_%d_%d_%d.xlsx", companyId, reportId, timestamp);
        return Paths.get(storagePath, fileName).toAbsolutePath();
    }

    private ReportResponseDto toResponseDto(Report r) {
        String username = null;
        if (r.getUserId() != null) {
            username = userRepository.findById(r.getUserId())
                    .map(u -> {
                        String email = u.getUsername();
                        if (email == null) return null;
                        int at = email.indexOf('@');
                        return at > 0 ? email.substring(0, at) : email;
                    })
                    .orElse(null);
        }
        return buildResponseDto(r, username);
    }

    private ReportResponseDto toResponseDto(Report r, Map<Long, User> userMap) {
        String username = null;
        if (r.getUserId() != null) {
            User u = userMap.get(r.getUserId());
            if (u != null) {
                String email = u.getUsername();
                if (email != null) {
                    int at = email.indexOf('@');
                    username = at > 0 ? email.substring(0, at) : email;
                }
            }
        }
        return buildResponseDto(r, username);
    }

    private ReportResponseDto buildResponseDto(Report r, String username) {
        return new ReportResponseDto(
                r.getReportId(),
                r.getReportType() != null ? r.getReportType().name() : null,
                r.getFormat() != null ? r.getFormat().name() : ReportFormat.EXCEL.name(),
                r.getStartDate(),
                r.getEndDate(),
                r.getGeneratedAt(),
                r.getFileSize(),
                username,
                r.isDeleted(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private BigDecimal computeIncomeTotal(Long companyId, ReportType type, LocalDate start, LocalDate end) {
        if (type == ReportType.EXPENSE) return BigDecimal.ZERO;
        BigDecimal invSum = invoiceRepository.sumPaidByTypeAndCreatedAtRange(
                companyId, InvoiceType.sale, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        BigDecimal txSum = sumTransactions(fetchTransactions(companyId, TransactionType.INCOME, start, end));
        return invSum.add(txSum);
    }

    private BigDecimal computeExpenseTotal(Long companyId, ReportType type, LocalDate start, LocalDate end) {
        if (type == ReportType.INCOME) return BigDecimal.ZERO;
        BigDecimal invSum = invoiceRepository.sumPaidByTypeAndCreatedAtRange(
                companyId, InvoiceType.purchase, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        BigDecimal txSum = sumTransactions(fetchTransactions(companyId, TransactionType.EXPENSE, start, end));
        return invSum.add(txSum);
    }

    private List<MonthlyPoint> buildMonthlySeries(Long companyId, ReportType type, LocalDate end) {
        YearMonth endMonth = YearMonth.from(end);
        List<MonthlyPoint> points = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = endMonth.minusMonths(i);
            LocalDate s = ym.atDay(1);
            LocalDate e = ym.atEndOfMonth();
            BigDecimal income = computeIncomeTotal(companyId, type, s, e);
            BigDecimal expense = computeExpenseTotal(companyId, type, s, e);
            points.add(new MonthlyPoint(monthLabel(ym), income, expense));
        }
        return points;
    }

    private String monthLabel(YearMonth ym) {
        String[] tr = {"Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"};
        return tr[ym.getMonthValue() - 1];
    }

    private List<Invoice> fetchPaidInvoices(Long companyId, InvoiceType type, LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.paid, type, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().isBefore(startDt)
                        && !i.getCreatedAt().isAfter(endDt))
                .collect(Collectors.toList());
    }

    private List<Transaction> fetchTransactions(Long companyId, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                        start, end, companyId)
                .stream()
                .filter(t -> t.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    private BigDecimal sumInvoices(List<Invoice> list) {
        return list.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumInvoiceVat(List<Invoice> list) {
        return list.stream()
                .map(i -> i.getVatAmount() != null ? i.getVatAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTransactions(List<Transaction> list) {
        return list.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Integer, Product> loadProductMap(Long companyId, List<Stock> stocks) {
        if (stocks.isEmpty()) return Map.of();
        List<Integer> ids = stocks.stream().map(Stock::getProductId).distinct().toList();
        return productRepository.findByProductIdInAndCompanyCompanyId(ids, companyId)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
    }

    // (kullanılmayan import uyarısını gidermek için; CustomerRepository ileride preview'de adı çekmek isterse)
    @SuppressWarnings("unused")
    private Optional<Customer> findCustomer(Long companyId, Long customerId) {
        if (customerId == null) return Optional.empty();
        return customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId);
    }
}
