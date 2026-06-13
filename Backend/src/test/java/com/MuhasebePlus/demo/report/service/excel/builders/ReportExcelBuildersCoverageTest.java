package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementLineDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementSectionDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryLineResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
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
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.IncomeExpenseSheetWriter;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportExcelBuildersCoverageTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Mock
    private ReportDataFetcher fetcher;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private JournalEntryService journalEntryService;

    private IncomeExpenseSheetWriter sheetWriter;
    private List<Stock> stocks;

    @BeforeEach
    void setUp() {
        sheetWriter = new IncomeExpenseSheetWriter();
        List<Invoice> paidSales = List.of(
                invoice(1L, 7L, InvoiceType.sale, PaymentStatus.paid, "S-1", "1000.00", "180.00", "820.00", START.plusDays(20)),
                invoice(2L, null, InvoiceType.sale, PaymentStatus.paid, null, null, null, null, null)
        );
        List<Invoice> paidPurchases = List.of(
                invoice(3L, 8L, InvoiceType.purchase, PaymentStatus.paid, "P-1", "250.00", "40.00", "210.00", START.plusDays(45))
        );
        List<Transaction> incomes = List.of(
                transaction(1L, TransactionType.INCOME, "300.00", START.plusDays(3), "Tahsilat", "Satis"),
                transaction(null, TransactionType.INCOME, null, null, null, null)
        );
        List<Transaction> expenses = List.of(
                transaction(2L, TransactionType.EXPENSE, "120.00", START.plusDays(5), "Kira", "Kira")
        );
        when(fetcher.fetchPaidInvoices(COMPANY_ID, InvoiceType.sale, START, END)).thenReturn(paidSales);
        when(fetcher.fetchPaidInvoices(COMPANY_ID, InvoiceType.purchase, START, END)).thenReturn(paidPurchases);
        when(fetcher.fetchTransactions(COMPANY_ID, TransactionType.INCOME, START, END)).thenReturn(incomes);
        when(fetcher.fetchTransactions(COMPANY_ID, TransactionType.EXPENSE, START, END)).thenReturn(expenses);

        Invoice pending = invoice(10L, 7L, InvoiceType.sale, PaymentStatus.pending, "S-A", "400.00", "72.00", "328.00", LocalDate.now().minusDays(20));
        Invoice overdue = invoice(11L, 8L, InvoiceType.sale, PaymentStatus.overdue, "S-B", "900.00", "162.00", "738.00", LocalDate.now().minusDays(120));
        Invoice noCustomer = invoice(12L, null, InvoiceType.sale, PaymentStatus.pending, "S-C", "50.00", "9.00", "41.00", null);
        ReflectionTestUtils.setField(pending, "createdAt", START.minusDays(10).atStartOfDay());
        ReflectionTestUtils.setField(overdue, "createdAt", START.plusDays(10).atStartOfDay());
        ReflectionTestUtils.setField(noCustomer, "createdAt", START.plusDays(20).atStartOfDay());
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending,
                InvoiceType.sale,
                COMPANY_ID
        )).thenReturn(List.of(pending, noCustomer));
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue,
                InvoiceType.sale,
                COMPANY_ID
        )).thenReturn(List.of(overdue));
        when(invoiceRepository.findByInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(InvoiceType.sale, COMPANY_ID))
                .thenReturn(List.of(pending, overdue, noCustomer));

        when(customerRepository.findAllById(any()))
                .thenReturn(List.of(customer(7L, "Ada Ltd."), customer(8L, "Bora A.S.")));

        stocks = List.of(
                stock(10, 5, 10, LocalDateTime.now().minusDays(35)),
                stock(20, 12, 5, LocalDateTime.now().minusDays(160)),
                stock(30, null, null, null)
        );
        Product firstProduct = product(10, "Laptop", "SKU-10", "adet", "1000.00", "700.00");
        Product secondProduct = product(20, "Monitor", null, null, null, null);
        when(stockRepository.findActiveStocks(COMPANY_ID)).thenReturn(stocks);
        when(fetcher.loadProductMap(COMPANY_ID, stocks))
                .thenReturn(Map.of(10, firstProduct, 20, secondProduct));
        when(invoiceLineItemRepository.sumQuantityByProductLast12Months(eq(COMPANY_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[] {10, 3L}, new Object[] {20, 0L}));
        when(invoiceLineItemRepository.findLastSaleDateByProductId(10, COMPANY_ID))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(30)));
        when(invoiceLineItemRepository.findLastSaleDateByProductId(20, COMPANY_ID))
                .thenReturn(Optional.empty());

        Budget planned = budget(2026, 5, "Kira", "100.00");
        Budget noCategory = budget(2026, 6, null, null);
        Budget outside = budget(2025, 12, "Eski", "999.00");
        when(budgetRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(planned, noCategory, outside));

        List<Transaction> bankAndBudgetTransactions = List.of(
                transaction(1L, TransactionType.INCOME, "200.00", START.plusDays(1), "", "Satis"),
                transaction(1L, TransactionType.INCOME, "200.00", START.plusDays(1), "", "Satis"),
                transaction(2L, TransactionType.EXPENSE, "75.00", START.plusDays(2), "Kira", ""),
                transaction(null, TransactionType.EXPENSE, null, START.plusDays(3), null, null)
        );
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                START,
                END,
                COMPANY_ID
        )).thenReturn(bankAndBudgetTransactions);
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(bankAccount(1L, "Garanti", "TR00"), bankAccount(2L, null, null)));

        when(journalEntryService.getTrialBalance(START, END)).thenReturn(List.of(
                trialRow("100", AccountType.ASSET, "1000.00", "0.00", "1000.00"),
                trialRow("320", null, "0.00", "1000.00", "-1000.00")
        ));
        FinancialStatementSectionDto incomeSection = section("600", "Gelirler", "1000.00");
        FinancialStatementSectionDto expenseSection = section("770", "Giderler", "300.00");
        when(journalEntryService.getIncomeStatement(START, END)).thenReturn(new IncomeStatementResponseDto(
                START,
                END,
                new BigDecimal("1000.00"),
                new BigDecimal("300.00"),
                new BigDecimal("700.00"),
                new BigDecimal("70.0"),
                List.of(incomeSection, expenseSection)
        ));
        when(journalEntryService.getBalanceSheet(END)).thenReturn(new BalanceSheetResponseDto(
                END,
                new BigDecimal("1500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                List.of(section("1", "Varliklar", "1500.00")),
                List.of(section("3", "Borclar", "500.00")),
                List.of(section("5", "Ozkaynak", "1000.00"))
        ));
        when(journalEntryService.list(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                journalEntry("YEV-1", START.plusDays(5), JournalSourceType.INVOICE),
                journalEntry("YEV-2", START.plusDays(7), null),
                journalEntry("YEV-OLD", START.minusDays(2), JournalSourceType.MANUAL),
                journalEntry("YEV-NODATE", null, JournalSourceType.MANUAL)
        )));
    }

    @Test
    void everyExcelBuilderCreatesExpectedWorkbookSheets() throws Exception {
        List<ReportExcelBuilder> builders = List.of(
                new IncomeExcelBuilder(fetcher, sheetWriter),
                new ExpenseExcelBuilder(fetcher, sheetWriter),
                new ProfitLossExcelBuilder(fetcher, sheetWriter),
                new CashFlowExcelBuilder(fetcher),
                new VatPrepExcelBuilder(fetcher),
                new StockStatusExcelBuilder(stockRepository, fetcher),
                new SlowInventoryExcelBuilder(stockRepository, invoiceLineItemRepository, fetcher),
                new ArAgingExcelBuilder(invoiceRepository, customerRepository),
                new CollectionPerformanceExcelBuilder(invoiceRepository, customerRepository),
                new BudgetVarianceExcelBuilder(budgetRepository, transactionRepository),
                new BankReconciliationExcelBuilder(transactionRepository, bankAccountRepository),
                new ExecutiveSummaryExcelBuilder(fetcher, invoiceRepository, invoiceLineItemRepository, stockRepository, customerRepository),
                new TrialBalanceExcelBuilder(journalEntryService),
                new IncomeStatementExcelBuilder(journalEntryService),
                new BalanceSheetExcelBuilder(journalEntryService),
                new JournalListingExcelBuilder(journalEntryService)
        );

        for (ReportExcelBuilder builder : builders) {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                builder.build(workbook, COMPANY_ID, START, END);

                assertThat(builder.supports()).isInstanceOf(ReportType.class);
                assertThat(workbook.getNumberOfSheets())
                        .as(builder.getClass().getSimpleName())
                        .isGreaterThan(0);
                assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows())
                        .as(builder.getClass().getSimpleName())
                        .isGreaterThan(0);
            }
        }
    }

    @Test
    void stockAndVatBuildersCoverFallbackAndPositiveVatBranches() throws Exception {
        when(fetcher.fetchPaidInvoices(COMPANY_ID, InvoiceType.sale, START, END))
                .thenReturn(List.of(invoice(20L, 9L, InvoiceType.sale, PaymentStatus.paid, "S-POS", "1000.00", "180.00", "820.00", START)));
        when(fetcher.fetchPaidInvoices(COMPANY_ID, InvoiceType.purchase, START, END))
                .thenReturn(List.of(invoice(21L, 9L, InvoiceType.purchase, PaymentStatus.paid, "P-LOW", "100.00", "18.00", "82.00", START)));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            new VatPrepExcelBuilder(fetcher).build(workbook, COMPANY_ID, START, END);

            assertThat(workbook.getSheetAt(0).getRow(5).getCell(0).getStringCellValue())
                    .contains("KDV");
        }

        when(fetcher.loadProductMap(COMPANY_ID, stocks)).thenReturn(Map.of());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            new StockStatusExcelBuilder(stockRepository, fetcher).build(workbook, COMPANY_ID, START, END);

            assertThat(workbook.getSheet("Stok Durum").getRow(1).getCell(0).getStringCellValue())
                    .contains("10");
        }
    }

    private Invoice invoice(Long id, Long customerId, InvoiceType type, PaymentStatus status, String number,
                            String total, String vat, String subtotal, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceType(type);
        invoice.setPaymentStatus(status);
        invoice.setInvoiceNumber(number);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(total != null ? new BigDecimal(total) : null);
        invoice.setVatAmount(vat != null ? new BigDecimal(vat) : null);
        invoice.setSubtotal(subtotal != null ? new BigDecimal(subtotal) : null);
        ReflectionTestUtils.setField(invoice, "createdAt", START.plusDays(1).atStartOfDay());
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

    private Product product(Integer id, String name, String barcode, String unit, String salePrice, String costPrice) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setBarcode(barcode);
        product.setUnit(unit);
        product.setSalePrice(salePrice != null ? new BigDecimal(salePrice) : null);
        product.setCostPrice(costPrice != null ? new BigDecimal(costPrice) : null);
        return product;
    }

    private Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setName(name);
        return customer;
    }

    private Budget budget(Integer year, Integer month, String category, String plannedAmount) {
        Budget budget = new Budget();
        budget.setYear(year);
        budget.setMonth(month);
        budget.setCategory(category);
        budget.setPlannedAmount(plannedAmount != null ? new BigDecimal(plannedAmount) : null);
        return budget;
    }

    private BankAccount bankAccount(Long id, String name, String iban) {
        BankAccount account = new BankAccount();
        account.setAccountId(id);
        account.setBankName(name);
        account.setIban(iban);
        return account;
    }

    private TrialBalanceRowDto trialRow(String code, AccountType type, String debit, String credit, String balance) {
        return new TrialBalanceRowDto(
                Long.valueOf(code),
                code,
                "Hesap " + code,
                type,
                new BigDecimal(debit),
                new BigDecimal(credit),
                new BigDecimal(balance)
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

    private JournalEntryResponseDto journalEntry(String number, LocalDate date, JournalSourceType sourceType) {
        JournalEntryLineResponseDto line = new JournalEntryLineResponseDto(
                1L,
                100L,
                "100",
                "Kasa",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                null,
                1
        );
        return new JournalEntryResponseDto(
                1L,
                number,
                date,
                "Fis",
                sourceType,
                99L,
                false,
                null,
                List.of(line),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
    }
}
