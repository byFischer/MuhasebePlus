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
import com.MuhasebePlus.demo.report.dto.response.ReportPreviewResponseDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.repository.ReportRepository;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilderRegistry;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceCoverageTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportExcelBuilderRegistry excelBuilderRegistry;
    @Mock
    private CompanyContext companyContext;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private JournalEntryService journalEntryService;
    @Mock
    private ReportExcelBuilder excelBuilder;

    private ReportService service;
    private Path storageDir;

    @BeforeEach
    void setUp() throws Exception {
        storageDir = Paths.get("target", "report-service-coverage").toAbsolutePath();
        Files.createDirectories(storageDir);
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
                storageDir.toString()
        );

        Company company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);

        User user = new User();
        user.setUserId(USER_ID);
        user.setEmail("raporcu@example.com");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        stubPreviewData();
    }

    @Test
    void generateReport_createsWorkbookFileAndPersistsMetadata() throws Exception {
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            if (report.getReportId() == null) {
                report.setReportId(222L);
            }
            return report;
        });
        when(excelBuilderRegistry.forType(ReportType.PROFIT_LOSS)).thenReturn(excelBuilder);
        doAnswer(invocation -> {
            Workbook workbook = invocation.getArgument(0);
            workbook.createSheet("Generated").createRow(0).createCell(0).setCellValue("ok");
            return null;
        }).when(excelBuilder).build(any(Workbook.class), eq(COMPANY_ID), eq(START), eq(END));

        ReportResponseDto result = service.generateReport(new ReportRequestDto(
                ReportType.PROFIT_LOSS,
                START,
                END,
                ReportFormat.EXCEL
        ));

        assertThat(result.reportId()).isEqualTo(222L);
        assertThat(result.fileSize()).isPositive();
        assertThat(result.createdByUsername()).isEqualTo("raporcu");
        try (Stream<Path> paths = Files.list(storageDir)) {
            assertThat(paths.map(path -> path.getFileName().toString()).toList())
                    .anySatisfy(fileName -> assertThat(fileName).startsWith("report_1_222_"));
        }
    }

    @Test
    void generateReport_whenDateRangeInvalid_throwsBeforeSaving() {
        ReportRequestDto request = new ReportRequestDto(
                ReportType.PROFIT_LOSS,
                END,
                START,
                ReportFormat.EXCEL
        );

        assertThatThrownBy(() -> service.generateReport(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tarihi");
    }

    @Test
    void previewReport_coversOperationalReportTypes() {
        List<ReportType> reportTypes = List.of(
                ReportType.INCOME,
                ReportType.EXPENSE,
                ReportType.VAT_PREP,
                ReportType.COLLECTION_PERFORMANCE,
                ReportType.SLOW_INVENTORY,
                ReportType.BUDGET_VARIANCE,
                ReportType.BANK_RECONCILIATION,
                ReportType.EXECUTIVE_SUMMARY,
                ReportType.JOURNAL_LISTING
        );

        for (ReportType reportType : reportTypes) {
            ReportPreviewResponseDto result = service.previewReport(new ReportPreviewRequestDto(reportType, START, END));

            assertThat(result.sections())
                    .as(reportType.name())
                    .isNotEmpty();
        }
    }

    @Test
    void previewReport_withZeroDataCoversNeutralBranches() {
        when(invoiceRepository.sumPaidByTypeAndCreatedAtRange(eq(COMPANY_ID), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                any(),
                any(),
                eq(COMPANY_ID)
        )).thenReturn(List.of());
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                any(),
                eq(InvoiceType.sale),
                eq(COMPANY_ID)
        )).thenAnswer(invocation -> new ArrayList<>());
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.paid,
                InvoiceType.sale,
                COMPANY_ID
        )).thenReturn(List.of());
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.paid,
                InvoiceType.purchase,
                COMPANY_ID
        )).thenReturn(List.of());
        when(invoiceRepository.findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of());
        when(stockRepository.findActiveStocks(COMPANY_ID)).thenReturn(List.of());
        when(productRepository.findByProductIdInAndCompanyCompanyId(any(), eq(COMPANY_ID))).thenReturn(List.of());
        when(invoiceLineItemRepository.sumQuantityByProductLast12Months(eq(COMPANY_ID), any())).thenReturn(List.of());
        when(invoiceLineItemRepository.findLastSaleDatesByProductIds(eq(COMPANY_ID), any())).thenReturn(List.of());
        when(budgetRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID)).thenReturn(List.of());
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID)).thenReturn(List.of());

        assertThat(service.previewReport(new ReportPreviewRequestDto(ReportType.PROFIT_LOSS, START, END))
                .sections().get(0).progressValue()).isEqualByComparingTo("0");
        assertThat(service.previewReport(new ReportPreviewRequestDto(ReportType.CASH_FLOW, START, END))
                .sections().get(0).kpis().get(2).tone()).isEqualTo("pos");
        assertThat(service.previewReport(new ReportPreviewRequestDto(ReportType.COLLECTION_PERFORMANCE, START, END))
                .sections().get(0).kpis().get(2).tone()).isEqualTo("pos");
        assertThat(service.previewReport(new ReportPreviewRequestDto(ReportType.BUDGET_VARIANCE, START, END))
                .sections().get(0).kpis().get(3).tone()).isEqualTo("pos");
        assertThat(service.previewReport(new ReportPreviewRequestDto(ReportType.BANK_RECONCILIATION, START, END))
                .sections().get(0).kpis().get(2).tone()).isEqualTo("pos");
    }

    @Test
    void downloadReport_readsFileAndReportsMissingCases() throws Exception {
        Path reportFile = storageDir.resolve("existing-report.xlsx");
        Files.write(reportFile, new byte[] {1, 2, 3, 4});
        Report withFile = report(10L);
        withFile.setFilePath(reportFile.toString());
        Report withoutFile = report(11L);
        withoutFile.setFilePath(null);
        Report unreadable = report(12L);
        unreadable.setFilePath(storageDir.resolve("missing-report.xlsx").toString());
        when(reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(withFile));
        when(reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(11L, COMPANY_ID))
                .thenReturn(Optional.of(withoutFile));
        when(reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(12L, COMPANY_ID))
                .thenReturn(Optional.of(unreadable));
        when(reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(service.downloadReport(10L)).containsExactly(1, 2, 3, 4);
        assertThatThrownBy(() -> service.downloadReport(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dosyas");
        assertThatThrownBy(() -> service.downloadReport(12L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("okunamad");
        assertThatThrownBy(() -> service.getReportById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    void hardDeleteExpired_removesStoredFileAndEntity() throws Exception {
        Path reportFile = storageDir.resolve("expired-report.xlsx");
        Files.writeString(reportFile, "expired");
        Report expired = report(55L);
        expired.setFilePath(reportFile.toString());
        when(reportRepository.findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime.of(2026, 6, 1, 0, 0)))
                .thenReturn(List.of(expired));

        int result = service.hardDeleteExpired(LocalDateTime.of(2026, 6, 1, 0, 0));

        assertThat(result).isEqualTo(1);
        assertThat(reportFile).doesNotExist();
        verify(reportRepository).delete(expired);
    }

    private void stubPreviewData() {
        when(invoiceRepository.sumPaidByTypeAndCreatedAtRange(eq(COMPANY_ID), eq(InvoiceType.sale), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(invoiceRepository.sumPaidByTypeAndCreatedAtRange(eq(COMPANY_ID), eq(InvoiceType.purchase), any(), any()))
                .thenReturn(new BigDecimal("350.00"));
        List<Transaction> transactions = List.of(
                transaction(1L, TransactionType.INCOME, "200.00", START.plusDays(1), "", "Satis"),
                transaction(1L, TransactionType.INCOME, "200.00", START.plusDays(1), "", "Satis"),
                transaction(2L, TransactionType.EXPENSE, "50.00", START.plusDays(2), "Kira", ""),
                transaction(null, TransactionType.EXPENSE, null, START.plusDays(3), null, null)
        );
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                any(),
                any(),
                eq(COMPANY_ID)
        )).thenReturn(transactions);

        Invoice paidSale = invoice(1L, 10L, InvoiceType.sale, PaymentStatus.paid, "1000.00", "180.00", START.plusDays(3));
        Invoice paidPurchase = invoice(2L, 20L, InvoiceType.purchase, PaymentStatus.paid, "300.00", "54.00", START.plusDays(4));
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.paid,
                InvoiceType.sale,
                COMPANY_ID
        )).thenReturn(List.of(paidSale));
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.paid,
                InvoiceType.purchase,
                COMPANY_ID
        )).thenReturn(List.of(paidPurchase));

        Invoice current = invoice(3L, 10L, InvoiceType.sale, PaymentStatus.pending, "400.00", "72.00", LocalDate.now().minusDays(10));
        Invoice old = invoice(4L, 20L, InvoiceType.sale, PaymentStatus.overdue, "900.00", "162.00", LocalDate.now().minusDays(120));
        ReflectionTestUtils.setField(current, "createdAt", START.minusDays(5).atStartOfDay());
        ReflectionTestUtils.setField(old, "createdAt", START.plusDays(10).atStartOfDay());
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending,
                InvoiceType.sale,
                COMPANY_ID
        )).thenAnswer(invocation -> new ArrayList<>(List.of(current)));
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue,
                InvoiceType.sale,
                COMPANY_ID
        )).thenReturn(List.of(old));
        when(invoiceRepository.findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of(current, old));

        List<Stock> stocks = List.of(
                stock(10, 2, 5, LocalDateTime.now().minusDays(35)),
                stock(20, 12, 3, LocalDateTime.now().minusDays(150)),
                stock(30, null, null, null)
        );
        when(stockRepository.findActiveStocks(COMPANY_ID)).thenReturn(stocks);
        when(productRepository.findByProductIdInAndCompanyCompanyId(any(), eq(COMPANY_ID)))
                .thenReturn(List.of(
                        product(10, "Laptop", "1000.00", "700.00"),
                        product(20, "Monitor", null, "200.00")
                ));
        when(invoiceLineItemRepository.findLastSaleDatesByProductIds(eq(COMPANY_ID), any()))
                .thenReturn(List.<Object[]>of(new Object[] {10, LocalDateTime.now().minusDays(30)}));
        when(invoiceLineItemRepository.sumQuantityByProductLast12Months(eq(COMPANY_ID), any()))
                .thenReturn(List.<Object[]>of(new Object[] {10, 4L}, new Object[] {20, 0L}));

        when(budgetRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(
                        budget(2026, 5, "Kira", "100.00"),
                        budget(2026, 6, null, null),
                        budget(2025, 12, "Eski", "999.00")
                ));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(bankAccount(1L, "Garanti"), bankAccount(2L, null)));

        when(journalEntryService.getTrialBalance(START, END)).thenReturn(List.of(
                trialRow("100", AccountType.ASSET, "1000.00"),
                trialRow("320", AccountType.LIABILITY, "-250.00"),
                trialRow("500", AccountType.EQUITY, "-750.00"),
                trialRow("600", AccountType.INCOME, "-1200.00"),
                trialRow("770", AccountType.EXPENSE, "300.00")
        ));
        FinancialStatementSectionDto section = section("600", "Gelirler", "1000.00");
        when(journalEntryService.getIncomeStatement(START, END)).thenReturn(new IncomeStatementResponseDto(
                START,
                END,
                new BigDecimal("1000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("700.00"),
                new BigDecimal("70.0"),
                List.of(section)
        ));
        when(journalEntryService.getBalanceSheet(END)).thenReturn(new BalanceSheetResponseDto(
                END,
                new BigDecimal("1000.00"),
                new BigDecimal("250.00"),
                new BigDecimal("750.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                List.of(section("1", "Varliklar", "1000.00")),
                List.of(section("3", "Borclar", "250.00")),
                List.of(section("5", "Ozkaynak", "750.00"))
        ));
    }

    private Report report(Long id) {
        Report report = new Report();
        report.setReportId(id);
        report.setUserId(USER_ID);
        report.setReportType(ReportType.PROFIT_LOSS);
        report.setFormat(ReportFormat.EXCEL);
        report.setStartDate(START);
        report.setEndDate(END);
        report.setGeneratedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        report.setFileSize(4L);
        return report;
    }

    private Invoice invoice(Long id, Long customerId, InvoiceType type, PaymentStatus status,
                            String total, String vat, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceType(type);
        invoice.setPaymentStatus(status);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(total != null ? new BigDecimal(total) : null);
        invoice.setVatAmount(vat != null ? new BigDecimal(vat) : null);
        ReflectionTestUtils.setField(invoice, "createdAt", START.plusDays(5).atStartOfDay());
        return invoice;
    }

    private Transaction transaction(Long accountId, TransactionType type, String amount,
                                    LocalDate date, String description, String category) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount != null ? new BigDecimal(amount) : null);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        transaction.setCategory(category);
        return transaction;
    }

    private Stock stock(Integer productId, Integer quantity, Integer minQuantity, LocalDateTime createdAt) {
        Stock stock = new Stock();
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setMinQuantity(minQuantity);
        ReflectionTestUtils.setField(stock, "createdAt", createdAt);
        return stock;
    }

    private Product product(Integer productId, String name, String salePrice, String costPrice) {
        Product product = new Product();
        product.setProductId(productId);
        product.setName(name);
        product.setSalePrice(salePrice != null ? new BigDecimal(salePrice) : null);
        product.setCostPrice(costPrice != null ? new BigDecimal(costPrice) : null);
        return product;
    }

    private Budget budget(Integer year, Integer month, String category, String plannedAmount) {
        Budget budget = new Budget();
        budget.setYear(year);
        budget.setMonth(month);
        budget.setCategory(category);
        budget.setPlannedAmount(plannedAmount != null ? new BigDecimal(plannedAmount) : null);
        return budget;
    }

    private BankAccount bankAccount(Long id, String bankName) {
        BankAccount account = new BankAccount();
        account.setAccountId(id);
        account.setBankName(bankName);
        return account;
    }

    private TrialBalanceRowDto trialRow(String code, AccountType type, String balance) {
        BigDecimal value = new BigDecimal(balance);
        return new TrialBalanceRowDto(
                Long.valueOf(code),
                code,
                "Hesap " + code,
                type,
                value.signum() > 0 ? value : BigDecimal.ZERO,
                value.signum() < 0 ? value.abs() : BigDecimal.ZERO,
                value
        );
    }

    private FinancialStatementSectionDto section(String code, String name, String total) {
        FinancialStatementLineDto line = new FinancialStatementLineDto(
                Long.valueOf(code),
                code,
                "Hesap " + code,
                AccountType.INCOME,
                BigDecimal.ZERO,
                new BigDecimal(total),
                new BigDecimal(total)
        );
        return new FinancialStatementSectionDto(code, name, new BigDecimal(total), List.of(line));
    }
}
