package com.MuhasebePlus.demo.report.service;

import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementLineDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementSectionDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
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
import com.MuhasebePlus.demo.report.dto.response.ReportPreviewResponseDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.repository.ReportRepository;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilderRegistry;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ReportExcelBuilderRegistry excelBuilderRegistry;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private JournalEntryService journalEntryService;

    private ReportService service;
    private Company company;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        service = new ReportService(
                reportRepository,
                excelBuilderRegistry,
                companyContext,
                companyRepository,
                userRepository,
                transactionRepository,
                invoiceRepository,
                invoiceLineItemRepository,
                customerRepository,
                stockRepository,
                productRepository,
                budgetRepository,
                bankAccountRepository,
                journalEntryService,
                "target/test-reports"
        );
    }

    // Tests report preview rejects inverted date ranges before reading repositories.
    @Test
    void previewReport_whenStartAfterEnd_throws() {
        assertThatThrownBy(() -> service.previewReport(preview(ReportType.PROFIT_LOSS,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tarihi");
    }

    // Tests profit/loss preview combines paid invoices and transactions.
    @Test
    void previewReport_profitLoss_calculatesIncomeExpenseNetAndMargin() {
        stubPaidInvoiceSums(new BigDecimal("1000.00"), new BigDecimal("300.00"));
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                any(), any(), eq(COMPANY_ID))).thenReturn(List.of(
                        transaction(TransactionType.INCOME, "200.00"),
                        transaction(TransactionType.EXPENSE, "50.00")
                ));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.PROFIT_LOSS));

        assertThat(result.sections()).hasSize(1);
        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("1200.00");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("350.00");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("850.00");
        assertThat(result.sections().get(0).progressValue()).isEqualByComparingTo("70.8");
        assertThat(result.sections().get(0).monthlySeries()).hasSize(12);
    }

    // Tests cash-flow preview uses income and expense cash transactions.
    @Test
    void previewReport_cashFlow_calculatesInflowOutflowAndNetCash() {
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                any(), any(), eq(COMPANY_ID))).thenReturn(List.of(
                        transaction(TransactionType.INCOME, "500.00"),
                        transaction(TransactionType.EXPENSE, "175.00")
                ));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.CASH_FLOW));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("500.00");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("175.00");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("325.00");
        assertThat(result.sections().get(0).monthlySeries()).hasSize(12);
    }

    // Tests AR aging preview buckets pending and overdue sales invoices by due date.
    @Test
    void previewReport_arAging_bucketsOpenReceivables() {
        Invoice current = invoice(1L, 10L, "100.00", LocalDate.now().minusDays(10), PaymentStatus.pending);
        Invoice mid = invoice(2L, 11L, "200.00", LocalDate.now().minusDays(45), PaymentStatus.overdue);
        Invoice old = invoice(3L, 10L, "300.00", LocalDate.now().minusDays(100), PaymentStatus.overdue);
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending, InvoiceType.sale, COMPANY_ID))
                .thenReturn(new ArrayList<>(List.of(current)));
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue, InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of(mid, old));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.AR_AGING));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("600.00");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("2");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("300.00");
        assertThat(result.sections().get(0).buckets().get(1).value()).isEqualByComparingTo("200.00");
        assertThat(result.sections().get(0).buckets().get(3).value()).isEqualByComparingTo("300.00");
    }

    // Tests stock status preview calculates low-stock count and stock value.
    @Test
    void previewReport_stockStatus_calculatesLowStockAndTotalValue() {
        Stock low = stock(5, 2, 5);
        Stock normal = stock(6, 10, 3);
        Product lowProduct = product(5, "25.00");
        Product normalProduct = product(6, "40.00");
        when(stockRepository.findActiveStocks(COMPANY_ID)).thenReturn(List.of(low, normal));
        when(productRepository.findByProductIdInAndCompanyCompanyId(List.of(5, 6), COMPANY_ID))
                .thenReturn(List.of(lowProduct, normalProduct));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.STOCK_STATUS));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("2");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("1");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("450.00");
        assertThat(result.sections().get(0).buckets().get(0).value()).isEqualByComparingTo("1");
    }

    // Tests trial balance preview derives totals and balanced percentage from journal service rows.
    @Test
    void previewReport_trialBalance_calculatesTotalsAndDifference() {
        when(journalEntryService.getTrialBalance(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(
                        trialRow(1L, "100", AccountType.ASSET, "1000.00", "0.00", "1000.00"),
                        trialRow(2L, "320", AccountType.LIABILITY, "0.00", "1000.00", "-1000.00")
                ));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.TRIAL_BALANCE));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("2");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("1000.00");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("1000.00");
        assertThat(result.sections().get(0).progressValue()).isEqualByComparingTo("100");
    }

    // Tests income statement preview delegates to accounting service when date range exists.
    @Test
    void previewReport_incomeStatement_usesAccountingStatementResponse() {
        FinancialStatementSectionDto section = new FinancialStatementSectionDto(
                "600",
                "Gelirler",
                new BigDecimal("1000.00"),
                List.of(new FinancialStatementLineDto(600L, "600", "Satislar", AccountType.INCOME,
                        BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("1000.00")))
        );
        when(journalEntryService.getIncomeStatement(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(new IncomeStatementResponseDto(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        new BigDecimal("1000.00"),
                        new BigDecimal("350.00"),
                        new BigDecimal("650.00"),
                        new BigDecimal("65.00"),
                        List.of(section)
                ));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.INCOME_STATEMENT));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("1000.00");
        assertThat(result.sections().get(0).kpis().get(2).value()).isEqualByComparingTo("650.00");
        assertThat(result.sections().get(0).progressValue()).isEqualByComparingTo("65.00");
        assertThat(result.sections().get(0).buckets()).hasSize(1);
    }

    // Tests balance sheet preview delegates to accounting service and exposes section buckets.
    @Test
    void previewReport_balanceSheet_usesAccountingBalanceSheetResponse() {
        FinancialStatementSectionDto assets = new FinancialStatementSectionDto(
                "1",
                "Donen Varliklar",
                new BigDecimal("2500.00"),
                List.of()
        );
        when(journalEntryService.getBalanceSheet(LocalDate.of(2026, 12, 31)))
                .thenReturn(new BalanceSheetResponseDto(
                        LocalDate.of(2026, 12, 31),
                        new BigDecimal("2500.00"),
                        new BigDecimal("700.00"),
                        new BigDecimal("1800.00"),
                        new BigDecimal("2500.00"),
                        BigDecimal.ZERO,
                        List.of(assets),
                        List.of(),
                        List.of()
                ));

        ReportPreviewResponseDto result = service.previewReport(preview(ReportType.BALANCE_SHEET));

        assertThat(result.sections().get(0).kpis().get(0).value()).isEqualByComparingTo("2500.00");
        assertThat(result.sections().get(0).kpis().get(1).value()).isEqualByComparingTo("2500.00");
        assertThat(result.sections().get(0).buckets()).hasSize(1);
        assertThat(result.sections().get(0).buckets().get(0).value()).isEqualByComparingTo("2500.00");
    }

    // Tests report list resolves creator username from email before @.
    @Test
    void getAllReports_whenUsersExist_mapsCreatorUsername() {
        Report report = report(90L);
        report.setUserId(7L);
        User user = new User();
        user.setUserId(7L);
        user.setEmail("ahmet@example.com");
        when(reportRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(COMPANY_ID))
                .thenReturn(List.of(report));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));

        List<ReportResponseDto> result = service.getAllReports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).createdByUsername()).isEqualTo("ahmet");
    }

    // Tests report soft delete marks deletion metadata.
    @Test
    void softDeleteReport_whenActiveReportExists_marksDeleted() {
        Report report = report(90L);
        when(reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(90L, COMPANY_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDeleteReport(90L);

        assertThat(report.isDeleted()).isTrue();
        assertThat(report.getDeletedAt()).isNotNull();
    }

    // Tests restore clears deleted metadata and maps response.
    @Test
    void restoreReport_whenDeletedReportExists_clearsDeletedFlag() {
        Report report = report(90L);
        report.setDeleted(true);
        report.setDeletedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        when(reportRepository.findByReportIdAndCompanyCompanyId(90L, COMPANY_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportResponseDto result = service.restoreReport(90L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(report.getDeletedAt()).isNull();
    }

    // Tests hard delete removes expired report records even when there is no file path.
    @Test
    void hardDeleteExpired_whenExpiredReportsExist_deletesRecordsAndReturnsCount() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        Report first = report(1L);
        Report second = report(2L);
        when(reportRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(reportRepository).delete(first);
        verify(reportRepository).delete(second);
    }

    private ReportPreviewRequestDto preview(ReportType type) {
        return preview(type, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    private ReportPreviewRequestDto preview(ReportType type, LocalDate start, LocalDate end) {
        return new ReportPreviewRequestDto(type, start, end);
    }

    private void stubPaidInvoiceSums(BigDecimal sale, BigDecimal purchase) {
        when(invoiceRepository.sumPaidByTypeAndCreatedAtRange(eq(COMPANY_ID), eq(InvoiceType.sale), any(), any()))
                .thenReturn(sale);
        when(invoiceRepository.sumPaidByTypeAndCreatedAtRange(eq(COMPANY_ID), eq(InvoiceType.purchase), any(), any()))
                .thenReturn(purchase);
    }

    private Transaction transaction(TransactionType type, String amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionDate(LocalDate.of(2026, 5, 20));
        return tx;
    }

    private Invoice invoice(Long id, Long customerId, String amount, LocalDate dueDate, PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceType(InvoiceType.sale);
        invoice.setPaymentStatus(status);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setDeleted(false);
        return invoice;
    }

    private Stock stock(Integer productId, Integer quantity, Integer minQuantity) {
        Stock stock = new Stock();
        stock.setCompany(company);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setMinQuantity(minQuantity);
        stock.setDeleted(false);
        return stock;
    }

    private Product product(Integer productId, String salePrice) {
        Product product = new Product();
        product.setProductId(productId);
        product.setCompany(company);
        product.setSalePrice(new BigDecimal(salePrice));
        product.setDeleted(false);
        return product;
    }

    private TrialBalanceRowDto trialRow(Long id, String code, AccountType type,
                                        String debit, String credit, String balance) {
        return new TrialBalanceRowDto(
                id,
                code,
                "Hesap " + code,
                type,
                new BigDecimal(debit),
                new BigDecimal(credit),
                new BigDecimal(balance)
        );
    }

    private Report report(Long id) {
        Report report = new Report();
        report.setReportId(id);
        report.setCompany(company);
        report.setReportType(ReportType.PROFIT_LOSS);
        report.setFormat(ReportFormat.EXCEL);
        report.setStartDate(LocalDate.of(2026, 1, 1));
        report.setEndDate(LocalDate.of(2026, 12, 31));
        report.setGeneratedAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        report.setFileSize(1024L);
        report.setDeleted(false);
        return report;
    }
}
