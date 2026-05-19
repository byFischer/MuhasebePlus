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
import com.MuhasebePlus.demo.report.service.excel.builders.BalanceSheetExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.BankReconciliationExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.BudgetVarianceExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.CashFlowExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.CollectionPerformanceExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.ExecutiveSummaryExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.ExpenseExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.IncomeExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.IncomeStatementExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.JournalListingExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.ProfitLossExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.SlowInventoryExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.StockStatusExcelBuilder;
import com.MuhasebePlus.demo.report.service.excel.builders.TrialBalanceExcelBuilder;
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
    private final BudgetVarianceExcelBuilder budgetVarianceBuilder;
    private final BankReconciliationExcelBuilder bankReconciliationBuilder;
    private final ExecutiveSummaryExcelBuilder executiveSummaryBuilder;
    private final TrialBalanceExcelBuilder trialBalanceBuilder;
    private final IncomeStatementExcelBuilder incomeStatementBuilder;
    private final BalanceSheetExcelBuilder balanceSheetBuilder;
    private final JournalListingExcelBuilder journalListingBuilder;
    private final StockStatusExcelBuilder stockStatusBuilder;

    public void build(ReportType type, Long companyId, LocalDate start, LocalDate end, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            switch (type) {
                case PROFIT_LOSS            -> profitLossBuilder.build(wb, companyId, start, end);
                case INCOME                 -> incomeBuilder.build(wb, companyId, start, end);
                case EXPENSE                -> expenseBuilder.build(wb, companyId, start, end);
                case CASH_FLOW              -> cashFlowBuilder.build(wb, companyId, start, end);
                case AR_AGING               -> arAgingBuilder.build(wb, companyId);
                case VAT_PREP               -> vatPrepBuilder.build(wb, companyId, start, end);
                case STOCK_STATUS           -> stockStatusBuilder.build(wb, companyId);
                case COLLECTION_PERFORMANCE -> collectionPerformanceBuilder.build(wb, companyId, start, end);
                case SLOW_INVENTORY        -> slowInventoryBuilder.build(wb, companyId);
                case BUDGET_VARIANCE    -> budgetVarianceBuilder.build(wb, companyId, start, end);
                case BANK_RECONCILIATION -> bankReconciliationBuilder.build(wb, companyId, start, end);
                case EXECUTIVE_SUMMARY  -> executiveSummaryBuilder.build(wb, companyId, start, end);
                case TRIAL_BALANCE      -> trialBalanceBuilder.build(wb, start, end);
                case INCOME_STATEMENT   -> incomeStatementBuilder.build(wb, start, end);
                case BALANCE_SHEET      -> balanceSheetBuilder.build(wb, start, end);
                case JOURNAL_LISTING    -> journalListingBuilder.build(wb, start, end);
            }
            wb.write(out);
        }
    }

}
