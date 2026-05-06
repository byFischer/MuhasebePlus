package com.MuhasebePlus.demo.dashboard.service.provider;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Component
public class BuiltInWidgetDataProvider implements WidgetDataProvider {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private TemplateRepository templateRepository;

    @Override
    public boolean supports(WidgetType type) {
        return switch (type) {
            case KPI, TIME_SERIES, PIE, TOP_N, ACTIVITY_FEED,
                 PROGRESS_GOAL, ANOMALY, TEMPLATES, MOOD_METER -> true;
            default -> false;
        };
    }

    @Override
    public Map<String, Object> fetchData(Long companyId, Map<String, Object> config) {
        throw new UnsupportedOperationException("Use typed fetch method via WidgetType");
    }

    public Map<String, Object> fetchData(WidgetType type, Long companyId, Map<String, Object> config) {
        return switch (type) {
            case KPI           -> fetchKpiData(companyId, config);
            case TIME_SERIES   -> fetchTimeSeriesData(companyId, config);
            case PIE           -> fetchPieData(companyId, config);
            case TOP_N         -> fetchTopNData(companyId, config);
            case ACTIVITY_FEED -> fetchActivityFeedData(companyId, config);
            case PROGRESS_GOAL -> fetchProgressGoalData(companyId, config);
            case ANOMALY       -> fetchAnomalyData(companyId, config);
            case TEMPLATES     -> fetchTemplatesData(companyId);
            case MOOD_METER    -> fetchMoodMeterData(companyId);
            default -> Map.of("message", "Built-in provider does not handle: " + type);
        };
    }

    private Map<String, Object> fetchKpiData(Long companyId, Map<String, Object> config) {
        String metric = getString(config, "metric", "net_revenue");
        DateRange range = resolveDateRange(config);

        return switch (metric) {
            case "net_revenue" -> {
                BigDecimal income = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.INCOME, range.start(), range.end(), companyId);
                BigDecimal expense = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.EXPENSE, range.start(), range.end(), companyId);
                yield Map.of("value", income.subtract(expense), "label", "Net Gelir", "currency", "TRY");
            }
            case "total_income" -> {
                BigDecimal val = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.INCOME, range.start(), range.end(), companyId);
                yield Map.of("value", val, "label", "Toplam Gelir", "currency", "TRY");
            }
            case "total_expense" -> {
                BigDecimal val = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.EXPENSE, range.start(), range.end(), companyId);
                yield Map.of("value", val, "label", "Toplam Gider", "currency", "TRY");
            }
            case "unpaid_invoices" -> {
                long count = invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.pending, companyId).size();
                yield Map.of("value", count, "label", "Ödenmemiş Fatura");
            }
            case "overdue_invoices" -> {
                long count = invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.overdue, companyId).size();
                yield Map.of("value", count, "label", "Vadesi Geçmiş Fatura");
            }
            default -> Map.of("value", 0, "label", metric);
        };
    }

    private Map<String, Object> fetchTimeSeriesData(Long companyId, Map<String, Object> config) {
        String dataSource = getString(config, "dataSource", "transactions");
        int months = getInt(config, "months", 6);

        List<Map<String, Object>> series = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("tr"));

            if ("transactions".equals(dataSource)) {
                BigDecimal income = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.INCOME, start, end, companyId);
                BigDecimal expense = transactionRepository.sumByTypeAndDateRange(
                        TransactionType.EXPENSE, start, end, companyId);
                series.add(Map.of(
                        "month", month.toString(),
                        "label", label,
                        "income", income,
                        "expense", expense,
                        "net", income.subtract(expense)
                ));
            } else if ("invoices".equals(dataSource)) {
                BigDecimal sales = invoiceRepository.sumByTypeAndDateRange(
                        InvoiceType.sale, start, end, companyId);
                BigDecimal purchases = invoiceRepository.sumByTypeAndDateRange(
                        InvoiceType.purchase, start, end, companyId);
                series.add(Map.of(
                        "month", month.toString(),
                        "label", label,
                        "sales", sales,
                        "purchases", purchases
                ));
            }
        }

        return Map.of("series", series, "dataSource", dataSource);
    }

    private Map<String, Object> fetchPieData(Long companyId, Map<String, Object> config) {
        String dataSource = getString(config, "dataSource", "transaction_types");
        DateRange range = resolveDateRange(config);

        if ("transaction_types".equals(dataSource)) {
            BigDecimal income = transactionRepository.sumByTypeAndDateRange(
                    TransactionType.INCOME, range.start(), range.end(), companyId);
            BigDecimal expense = transactionRepository.sumByTypeAndDateRange(
                    TransactionType.EXPENSE, range.start(), range.end(), companyId);
            return Map.of("slices", List.of(
                    Map.of("name", "Gelir", "value", income),
                    Map.of("name", "Gider", "value", expense)
            ));
        }

        if ("invoice_types".equals(dataSource)) {
            BigDecimal sales = invoiceRepository.sumByTypeAndDateRange(
                    InvoiceType.sale, range.start(), range.end(), companyId);
            BigDecimal purchases = invoiceRepository.sumByTypeAndDateRange(
                    InvoiceType.purchase, range.start(), range.end(), companyId);
            return Map.of("slices", List.of(
                    Map.of("name", "Satış", "value", sales),
                    Map.of("name", "Alış", "value", purchases)
            ));
        }

        return Map.of("slices", List.of());
    }

    private Map<String, Object> fetchTopNData(Long companyId, Map<String, Object> config) {
        int limit = getInt(config, "limit", 5);
        String dimension = getString(config, "dimension", "customers");

        if ("customers".equals(dimension)) {
            var items = invoiceRepository.findTopCustomersByRevenue(companyId)
                    .stream().limit(limit)
                    .map(row -> Map.<String, Object>of(
                            "name", row[1] != null ? row[1].toString() : "Bilinmiyor",
                            "value", row[2]
                    )).toList();
            return Map.of("items", items, "dimension", "Müşteri");
        }

        return Map.of("items", List.of(), "dimension", dimension);
    }

    private Map<String, Object> fetchActivityFeedData(Long companyId, Map<String, Object> config) {
        int limit = getInt(config, "limit", 10);
        var items = transactionRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(companyId)
                .stream().limit(limit)
                .map(t -> Map.<String, Object>of(
                        "id", t.getTransactionId(),
                        "type", t.getTransactionType(),
                        "amount", t.getAmount(),
                        "date", t.getTransactionDate().toString(),
                        "description", t.getDescription() != null ? t.getDescription() : ""
                )).toList();
        return Map.of("transactions", items);
    }

    private Map<String, Object> fetchProgressGoalData(Long companyId, Map<String, Object> config) {
        DateRange range = resolveDateRange(config);
        BigDecimal goal = new BigDecimal(getString(config, "goal", "10000"));
        String metric = getString(config, "metric", "total_income");

        BigDecimal current = "total_income".equals(metric)
                ? transactionRepository.sumByTypeAndDateRange(TransactionType.INCOME, range.start(), range.end(), companyId)
                : transactionRepository.sumByTypeAndDateRange(TransactionType.EXPENSE, range.start(), range.end(), companyId);

        double progress = goal.compareTo(BigDecimal.ZERO) > 0
                ? current.divide(goal, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return Map.of("current", current, "goal", goal, "progress", Math.min(progress, 100));
    }

    private Map<String, Object> fetchAnomalyData(Long companyId, Map<String, Object> config) {
        int lookbackDays = getInt(config, "lookbackDays", 30);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(lookbackDays);

        var transactions = transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                        start, end, companyId);

        if (transactions.isEmpty()) return Map.of("anomalies", List.of());

        double avg = transactions.stream().mapToDouble(t -> t.getAmount().doubleValue()).average().orElse(0);
        double variance = transactions.stream()
                .mapToDouble(t -> Math.pow(t.getAmount().doubleValue() - avg, 2)).average().orElse(0);
        double std = Math.sqrt(variance);
        double threshold = avg + 2 * std;

        var anomalies = transactions.stream()
                .filter(t -> t.getAmount().doubleValue() > threshold)
                .limit(10)
                .map(t -> Map.<String, Object>of(
                        "id", t.getTransactionId(),
                        "amount", t.getAmount(),
                        "date", t.getTransactionDate().toString(),
                        "type", t.getTransactionType().name(),
                        "zscore", std > 0 ? (t.getAmount().doubleValue() - avg) / std : 0
                )).toList();

        return Map.of("anomalies", anomalies, "threshold", threshold, "mean", avg, "std", std);
    }

    private Map<String, Object> fetchTemplatesData(Long companyId) {
        var templates = templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(companyId);
        List<Map<String, Object>> items = templates.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("templateId", t.getTemplateId());
            map.put("templateCode", t.getTemplateCode());
            map.put("templateName", t.getTemplateName());
            map.put("templateType", t.getTemplateType() != null ? t.getTemplateType().name() : null);
            map.put("description", t.getDescription());
            map.put("period", t.getPeriod());
            if (t.getPayload() != null) map.put("payload", t.getPayload());
            return map;
        }).toList();
        return Map.of("templates", items);
    }

    private Map<String, Object> fetchMoodMeterData(Long companyId) {
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = current.atEndOfMonth();

        BigDecimal income = transactionRepository.sumByTypeAndDateRange(TransactionType.INCOME, start, end, companyId);
        BigDecimal expense = transactionRepository.sumByTypeAndDateRange(TransactionType.EXPENSE, start, end, companyId);
        long overdueCount = invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue, companyId).size();

        double incomeD = income.doubleValue();
        double expenseD = expense.doubleValue();
        double netRatio = (incomeD + expenseD) > 0 ? incomeD / (incomeD + expenseD) : 0.5;
        double overduePenalty = Math.min(overdueCount * 0.05, 0.3);
        int score = (int) Math.round((netRatio - overduePenalty) * 100);
        score = Math.max(0, Math.min(100, score));

        String mood = score >= 75 ? "excellent" : score >= 50 ? "good" : score >= 25 ? "fair" : "poor";

        return Map.of("score", score, "mood", mood,
                "income", income, "expense", expense, "overdueInvoices", overdueCount);
    }

    private DateRange resolveDateRange(Map<String, Object> config) {
        String preset = getString(config, "dateRange", "THIS_MONTH");
        YearMonth current = YearMonth.now();
        return switch (preset) {
            case "LAST_MONTH"    -> {
                YearMonth last = current.minusMonths(1);
                yield new DateRange(last.atDay(1), last.atEndOfMonth());
            }
            case "LAST_3_MONTHS" -> new DateRange(current.minusMonths(2).atDay(1), current.atEndOfMonth());
            case "LAST_6_MONTHS" -> new DateRange(current.minusMonths(5).atDay(1), current.atEndOfMonth());
            case "THIS_YEAR"     -> new DateRange(LocalDate.of(current.getYear(), 1, 1), LocalDate.of(current.getYear(), 12, 31));
            default              -> new DateRange(current.atDay(1), current.atEndOfMonth());
        };
    }

    private String getString(Map<String, Object> config, String key, String defaultValue) {
        Object val = config.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object val = config.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception e) { return defaultValue; } }
        return defaultValue;
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
