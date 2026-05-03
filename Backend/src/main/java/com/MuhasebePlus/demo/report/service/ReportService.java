
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
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
import com.MuhasebePlus.demo.user.repository.UserRepository;

@Service
@Transactional
public class ReportService implements HardDeletable {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportExcelBuilder excelBuilder;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineItemRepository invoiceLineItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductRepository productRepository;

    @Value("${app.report.storage-path:./reports/}")
    private String storagePath;


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
        return reportRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
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
            case BUDGET_VARIANCE, BANK_RECONCILIATION, EXECUTIVE_SUMMARY ->
                    notImplementedSection(dto.reportType());
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

            List<Object[]> salesData = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
            for (Object[] row : salesData) {
                if (row[0].equals(stock.getProductId())) {
                    totalSoldQty12Months = totalSoldQty12Months.add(BigDecimal.valueOf(((Number) row[1]).longValue()));
                    productsWithSales++;
                    break;
                }
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
        BigDecimal invSum = sumInvoices(fetchPaidInvoices(companyId, InvoiceType.sale, start, end));
        BigDecimal txSum = sumTransactions(fetchTransactions(companyId, TransactionType.INCOME, start, end));
        return invSum.add(txSum);
    }

    private BigDecimal computeExpenseTotal(Long companyId, ReportType type, LocalDate start, LocalDate end) {
        if (type == ReportType.INCOME) return BigDecimal.ZERO;
        BigDecimal invSum = sumInvoices(fetchPaidInvoices(companyId, InvoiceType.purchase, start, end));
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
