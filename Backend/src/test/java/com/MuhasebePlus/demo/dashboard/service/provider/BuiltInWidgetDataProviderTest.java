package com.MuhasebePlus.demo.dashboard.service.provider;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuiltInWidgetDataProviderTest {

    private static final Long COMPANY_ID = 10L;

    @Mock private TransactionRepository transactionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TemplateRepository templateRepository;

    @InjectMocks private BuiltInWidgetDataProvider provider;

    @Test
    void supports_onlyBuiltInWidgetTypes() {
        assertThat(provider.supports(WidgetType.KPI)).isTrue();
        assertThat(provider.supports(WidgetType.DATA_QUERY)).isFalse();
    }

    @Test
    void fetchData_withoutWidgetType_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> provider.fetchData(COMPANY_ID, Map.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("typed fetch");
    }

    @Test
    void fetchKpiData_returnsFinancialMetricsAndInvoiceCounts() {
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("1500.00"));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("400.00"));
        when(invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(PaymentStatus.pending, COMPANY_ID))
                .thenReturn(List.of(new Invoice(), new Invoice()));
        when(invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(PaymentStatus.overdue, COMPANY_ID))
                .thenReturn(List.of(new Invoice()));

        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "net_revenue", "dateRange", "LAST_MONTH")))
                .containsEntry("value", new BigDecimal("1100.00"))
                .containsEntry("label", "Net Gelir")
                .containsEntry("currency", "TRY");
        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "total_income")))
                .containsEntry("value", new BigDecimal("1500.00"))
                .containsEntry("label", "Toplam Gelir");
        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "total_expense")))
                .containsEntry("value", new BigDecimal("400.00"))
                .containsEntry("label", "Toplam Gider");
        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "unpaid_invoices")))
                .containsEntry("value", 2L);
        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "overdue_invoices")))
                .containsEntry("value", 1L);
        assertThat(provider.fetchData(WidgetType.KPI, COMPANY_ID, Map.of("metric", "unknown_metric")))
                .containsEntry("value", 0)
                .containsEntry("label", "unknown_metric");
    }

    @Test
    void fetchTimeSeriesData_supportsTransactionsInvoicesAndInvalidMonthString() {
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("900.00"));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("250.00"));
        when(invoiceRepository.sumByTypeAndDateRange(eq(InvoiceType.sale), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("700.00"));
        when(invoiceRepository.sumByTypeAndDateRange(eq(InvoiceType.purchase), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("300.00"));

        Map<String, Object> transactions = provider.fetchData(
                WidgetType.TIME_SERIES, COMPANY_ID, Map.of("dataSource", "transactions", "months", "bad"));
        Map<String, Object> invoices = provider.fetchData(
                WidgetType.TIME_SERIES, COMPANY_ID, Map.of("dataSource", "invoices", "months", 2));

        assertThat(transactions).containsEntry("dataSource", "transactions");
        assertThat((List<?>) transactions.get("series")).hasSize(6);
        assertThat((List<?>) invoices.get("series")).hasSize(2);
        assertThat(((List<?>) invoices.get("series")).getFirst())
                .asString()
                .contains("sales", "purchases");
    }

    @Test
    void fetchPieData_supportsTransactionInvoiceAndUnknownSources() {
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("100.00"));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("60.00"));
        when(invoiceRepository.sumByTypeAndDateRange(eq(InvoiceType.sale), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("80.00"));
        when(invoiceRepository.sumByTypeAndDateRange(eq(InvoiceType.purchase), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("40.00"));

        assertThat((List<?>) provider.fetchData(WidgetType.PIE, COMPANY_ID, Map.of("dataSource", "transaction_types", "dateRange", "LAST_3_MONTHS")).get("slices"))
                .hasSize(2);
        assertThat((List<?>) provider.fetchData(WidgetType.PIE, COMPANY_ID, Map.of("dataSource", "invoice_types", "dateRange", "THIS_YEAR")).get("slices"))
                .hasSize(2);
        assertThat((List<?>) provider.fetchData(WidgetType.PIE, COMPANY_ID, Map.of("dataSource", "unknown")).get("slices"))
                .isEmpty();
    }

    @Test
    void fetchTopNData_limitsCustomerRowsAndHandlesUnknownDimension() {
        when(invoiceRepository.findTopCustomersByRevenue(COMPANY_ID)).thenReturn(List.of(
                new Object[] {1L, "Ada Ltd", new BigDecimal("1000")},
                new Object[] {2L, null, new BigDecimal("500")}
        ));

        Map<String, Object> customers = provider.fetchData(
                WidgetType.TOP_N, COMPANY_ID, Map.of("dimension", "customers", "limit", "1"));
        Map<String, Object> unknown = provider.fetchData(
                WidgetType.TOP_N, COMPANY_ID, Map.of("dimension", "products"));

        assertThat((List<?>) customers.get("items")).hasSize(1);
        assertThat(customers).containsEntry("dimension", "Müşteri");
        assertThat((List<?>) unknown.get("items")).isEmpty();
        assertThat(unknown).containsEntry("dimension", "products");
    }

    @Test
    void fetchActivityFeedData_mapsRecentTransactions() {
        Transaction transaction = transaction(7L, TransactionType.INCOME, "123.45", "Tahsilat");
        when(transactionRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(COMPANY_ID))
                .thenReturn(List.of(transaction, transaction(8L, TransactionType.EXPENSE, "20.00", null)));

        Map<String, Object> data = provider.fetchData(WidgetType.ACTIVITY_FEED, COMPANY_ID, Map.of("limit", 1));

        assertThat((List<?>) data.get("transactions")).hasSize(1);
        assertThat(((List<?>) data.get("transactions")).getFirst())
                .asString()
                .contains("Tahsilat", "123.45");
    }

    @Test
    void fetchProgressGoalData_capsProgressAndHandlesExpenseMetricAndZeroGoal() {
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("15000.00"));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("2500.00"));

        Map<String, Object> incomeGoal = provider.fetchData(
                WidgetType.PROGRESS_GOAL, COMPANY_ID, Map.of("goal", "10000", "metric", "total_income", "dateRange", "LAST_6_MONTHS"));
        Map<String, Object> expenseGoal = provider.fetchData(
                WidgetType.PROGRESS_GOAL, COMPANY_ID, Map.of("goal", "10000", "metric", "total_expense"));
        Map<String, Object> zeroGoal = provider.fetchData(
                WidgetType.PROGRESS_GOAL, COMPANY_ID, Map.of("goal", "0"));

        assertThat(incomeGoal).containsEntry("progress", 100.0);
        assertThat(expenseGoal).containsEntry("current", new BigDecimal("2500.00"));
        assertThat(zeroGoal).containsEntry("progress", 0.0);
    }

    @Test
    void fetchAnomalyData_returnsEmptyOrDetectedAnomalies() {
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        transaction(1L, TransactionType.EXPENSE, "100.00", "normal"),
                        transaction(2L, TransactionType.EXPENSE, "100.00", "normal"),
                        transaction(3L, TransactionType.EXPENSE, "100.00", "normal"),
                        transaction(4L, TransactionType.EXPENSE, "1000.00", "large")
                ));

        assertThat((List<?>) provider.fetchData(WidgetType.ANOMALY, COMPANY_ID, Map.of()).get("anomalies"))
                .isEmpty();
        Map<String, Object> detected = provider.fetchData(WidgetType.ANOMALY, COMPANY_ID, Map.of("lookbackDays", "7"));

        assertThat(detected).containsKeys("threshold", "mean", "std", "anomalies");
    }

    @Test
    void fetchTemplatesAndMoodMeterData_mapDomainValues() {
        Template template = new Template();
        template.setTemplateId(3L);
        template.setTemplateCode("TMP");
        template.setTemplateName("Tahsilat");
        template.setTemplateType(TemplateType.INCOME);
        template.setDescription("Aciklama");
        template.setPeriod("MONTHLY");
        template.setPayload(Map.of("account", "100"));
        when(templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(COMPANY_ID))
                .thenReturn(List.of(template));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("900.00"));
        when(transactionRepository.sumByTypeAndDateRange(eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class), eq(COMPANY_ID)))
                .thenReturn(new BigDecimal("100.00"));
        when(invoiceRepository.findByPaymentStatusAndCompanyCompanyIdAndIsDeletedFalse(PaymentStatus.overdue, COMPANY_ID))
                .thenReturn(List.of(new Invoice(), new Invoice()));

        Map<String, Object> templates = provider.fetchData(WidgetType.TEMPLATES, COMPANY_ID, Map.of());
        Map<String, Object> mood = provider.fetchData(WidgetType.MOOD_METER, COMPANY_ID, Map.of());
        Map<String, Object> unsupported = provider.fetchData(WidgetType.DATA_CHART, COMPANY_ID, Map.of());

        assertThat((List<?>) templates.get("templates")).hasSize(1);
        assertThat(mood).containsEntry("mood", "excellent").containsEntry("overdueInvoices", 2L);
        assertThat(unsupported.get("message")).asString().contains("DATA_CHART");
    }

    private Transaction transaction(Long id, TransactionType type, String amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setTransactionType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionDate(LocalDate.of(2026, 6, 1));
        transaction.setDescription(description);
        return transaction;
    }
}
