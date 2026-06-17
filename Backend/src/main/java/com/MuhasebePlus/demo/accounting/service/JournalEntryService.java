package com.MuhasebePlus.demo.accounting.service;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.BalanceSheetResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementLineDto;
import com.MuhasebePlus.demo.accounting.dto.response.FinancialStatementSectionDto;
import com.MuhasebePlus.demo.accounting.dto.response.GeneralLedgerRowDto;
import com.MuhasebePlus.demo.accounting.dto.response.IncomeStatementResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryLineResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.*;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntrySequenceRepository;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.fixedasset.entity.AssetCategory;
import com.MuhasebePlus.demo.fixedasset.entity.DepreciationLine;
import com.MuhasebePlus.demo.fixedasset.entity.FixedAsset;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JournalEntryService {

    private final JournalEntryRepository entryRepository;
    private final JournalEntryLineRepository lineRepository;
    private final JournalEntrySequenceRepository sequenceRepository;
    private final ChartOfAccountService chartOfAccountService;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ProductRepository productRepository;
    private final CompanyContext companyContext;
    private final AccountingPeriodGuard periodGuard;

    // ─── Invoice hooks ───────────────────────────────────────────────────────

    public void createForInvoice(Invoice invoice) {
        Long companyId = invoice.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        periodGuard.assertOpen(invoice.getInvoiceDate());
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.INVOICE, invoice.getInvoiceId())) return;

        boolean isSale = invoice.getInvoiceType() == InvoiceType.sale;
        BigDecimal subtotal = nvl(invoice.getSubtotal());
        BigDecimal vatAmount = nvl(invoice.getVatAmount());
        BigDecimal withholding = nvl(invoice.getWithholdingTaxAmount());
        BigDecimal totalAmount = nvl(invoice.getTotalAmount());

        List<JournalEntryLine> lines = new ArrayList<>();
        Company company = invoice.getCompany();
        int order = 0;

        if (isSale) {
            // Debit: Customer receivable (120.xx.xxx or 120)
            String custCode = resolveCustomerCode(companyId, invoice.getCustomerId(), false);
            lines.add(line(company, accountId(companyId, custCode), totalAmount, BigDecimal.ZERO, "Müşteri alacağı", order++));
            if (withholding.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(company, accountId(companyId, "360"), withholding, BigDecimal.ZERO, "Stopaj", order++));
            }
            // Credit: Sales revenue (600)
            lines.add(line(company, accountId(companyId, "600"), BigDecimal.ZERO, subtotal, "Satış geliri", order++));
            if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(company, accountId(companyId, "391"), BigDecimal.ZERO, vatAmount, "Hesaplanan KDV", order++));
            }
            BigDecimal stockCost = calculateInvoiceStockCost(companyId, invoice.getInvoiceId());
            if (stockCost.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(company, accountId(companyId, "621"), stockCost, BigDecimal.ZERO, "Satılan ticari mal maliyeti", order++));
                lines.add(line(company, accountId(companyId, "153"), BigDecimal.ZERO, stockCost, "Stok çıkışı", order++));
            }
        } else {
            // Debit: Goods (153)
            lines.add(line(company, accountId(companyId, "153"), subtotal, BigDecimal.ZERO, "Mal alışı", order++));
            if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(line(company, accountId(companyId, "191"), vatAmount, BigDecimal.ZERO, "İndirilecek KDV", order++));
            }
            // Credit: Vendor payable (320.xx.xxx or 320)
            String vendorCode = resolveCustomerCode(companyId, invoice.getCustomerId(), true);
            lines.add(line(company, accountId(companyId, vendorCode), BigDecimal.ZERO, totalAmount, "Satıcı borcu", order++));
        }

        save(company, invoice.getInvoiceDate(),
                (isSale ? "Satış faturası: " : "Alış faturası: ") + invoice.getInvoiceNumber(),
                JournalSourceType.INVOICE, invoice.getInvoiceId(), lines);
    }

    public void reverseForInvoice(Long companyId, Long invoiceId, String reason) {
        reverseForSource(companyId, JournalSourceType.INVOICE, invoiceId, reason);
    }

    // ─── Payment hooks ────────────────────────────────────────────────────────

    public void createForPayment(InvoicePayment payment, Transaction transaction) {
        Long companyId = payment.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        periodGuard.assertOpen(payment.getPaymentDate());
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.PAYMENT, payment.getPaymentId())) return;

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new BusinessException("Fatura bulunamadı: " + payment.getInvoiceId()));
        boolean isSalePayment = invoice.getInvoiceType() == InvoiceType.sale;
        BigDecimal amount = payment.getAmount();
        Company company = payment.getCompany();

        String bankCode = resolveBankAccountCode(companyId, payment.getBankAccountId());
        String custCode = resolveCustomerCode(companyId, invoice.getCustomerId(), !isSalePayment);

        List<JournalEntryLine> lines = new ArrayList<>();
        if (isSalePayment) {
            // Debit: Bank/Cash, Credit: Customer
            lines.add(line(company, accountId(companyId, bankCode), amount, BigDecimal.ZERO, "Tahsilat", 0));
            lines.add(line(company, accountId(companyId, custCode), BigDecimal.ZERO, amount, "Müşteri alacağı kapatıldı", 1));
        } else {
            // Debit: Vendor, Credit: Bank/Cash
            lines.add(line(company, accountId(companyId, custCode), amount, BigDecimal.ZERO, "Satıcı borcu ödendi", 0));
            lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, "Ödeme", 1));
        }

        save(company, payment.getPaymentDate(), "Ödeme: " + invoice.getInvoiceNumber(),
                JournalSourceType.PAYMENT, payment.getPaymentId(), lines);
    }

    public void reverseForPayment(Long companyId, Long paymentId, String reason) {
        reverseForSource(companyId, JournalSourceType.PAYMENT, paymentId, reason);
    }

    public void createForChequePayment(InvoicePayment payment, Cheque cheque) {
        Long companyId = payment.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        periodGuard.assertOpen(payment.getPaymentDate());
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.PAYMENT, payment.getPaymentId())) return;

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new BusinessException("Fatura bulunamadÄ±: " + payment.getInvoiceId()));
        boolean isSalePayment = invoice.getInvoiceType() == InvoiceType.sale;
        BigDecimal amount = nvl(payment.getAmount());
        Company company = payment.getCompany();

        String chequeCode = resolveChequeAccountCode(cheque);
        String custCode = resolveCustomerCode(companyId, invoice.getCustomerId(), !isSalePayment);

        List<JournalEntryLine> lines = new ArrayList<>();
        if (isSalePayment) {
            lines.add(line(company, accountId(companyId, chequeCode), amount, BigDecimal.ZERO, "Cek portfoye alindi", 0));
            lines.add(line(company, accountId(companyId, custCode), BigDecimal.ZERO, amount, "Musteri alacagi kapatildi", 1));
        } else {
            lines.add(line(company, accountId(companyId, custCode), amount, BigDecimal.ZERO, "Satici borcu cek ile odendi", 0));
            lines.add(line(company, accountId(companyId, chequeCode), BigDecimal.ZERO, amount, "Verilen cek", 1));
        }

        save(company, payment.getPaymentDate(), "Cekli odeme: " + invoice.getInvoiceNumber(),
                JournalSourceType.PAYMENT, payment.getPaymentId(), lines);
    }

    public void createForChequeSettlement(Cheque cheque, Transaction transaction) {
        Long companyId = cheque.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.CHEQUE, cheque.getChequeId())) return;

        BigDecimal amount = nvl(cheque.getAmount());
        Company company = cheque.getCompany();
        String chequeCode = resolveChequeAccountCode(cheque);
        String bankCode = resolveBankAccountCode(companyId,
                transaction != null ? transaction.getAccountId() : cheque.getBankAccountId());

        List<JournalEntryLine> lines = new ArrayList<>();
        if (cheque.getChequeType() == ChequeType.PAYABLE) {
            lines.add(line(company, accountId(companyId, chequeCode), amount, BigDecimal.ZERO, "Verilen cek odendi", 0));
            lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, "Banka cikisi", 1));
        } else {
            lines.add(line(company, accountId(companyId, bankCode), amount, BigDecimal.ZERO, "Cek tahsilati", 0));
            lines.add(line(company, accountId(companyId, chequeCode), BigDecimal.ZERO, amount, "Portfoy cek tahsil edildi", 1));
        }

        LocalDate entryDate = transaction != null && transaction.getTransactionDate() != null
                ? transaction.getTransactionDate()
                : LocalDate.now();
        periodGuard.assertOpen(entryDate);
        save(company, entryDate, "Cek tahsil/odeme: " + cheque.getChequeNumber(),
                JournalSourceType.CHEQUE, cheque.getChequeId(), lines);
    }

    // ─── Transaction hooks ────────────────────────────────────────────────────

    public void createForTransaction(Transaction tx) {
        Long companyId = tx.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        periodGuard.assertOpen(tx.getTransactionDate());
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.TRANSACTION, tx.getTransactionId())) return;

        BigDecimal amount = tx.getAmount();
        Company company = tx.getCompany();
        String bankCode = resolveBankAccountCode(companyId, tx.getAccountId());
        TransactionCategory category = tx.getTransactionCategory() != null
                ? tx.getTransactionCategory()
                : TransactionCategory.GENERAL;

        List<JournalEntryLine> lines = new ArrayList<>();
        switch (category) {
            case TRANSFER -> {
                if (tx.getTransferAccountId() == null) {
                    throw new BusinessException("Virman fisi icin hedef hesap zorunludur.");
                }
                String targetCode = resolveBankAccountCode(companyId, tx.getTransferAccountId());
                lines.add(line(company, accountId(companyId, targetCode), amount, BigDecimal.ZERO, "Virman girisi", 0));
                lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, "Virman cikisi", 1));
            }
            case BANK_FEE -> {
                lines.add(line(company, accountId(companyId, "653"), amount, BigDecimal.ZERO, tx.getDescription(), 0));
                lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, tx.getDescription(), 1));
            }
            case CREDIT_CARD_PAYMENT -> {
                lines.add(line(company, accountId(companyId, "300"), amount, BigDecimal.ZERO, tx.getDescription(), 0));
                lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, tx.getDescription(), 1));
            }
            case POS_COLLECTION, GENERAL -> {
                if (tx.getTransactionType() == TransactionType.INCOME) {
                    lines.add(line(company, accountId(companyId, bankCode), amount, BigDecimal.ZERO, tx.getDescription(), 0));
                    lines.add(line(company, accountId(companyId, "602"), BigDecimal.ZERO, amount, tx.getDescription(), 1));
                } else {
                    lines.add(line(company, accountId(companyId, "632"), amount, BigDecimal.ZERO, tx.getDescription(), 0));
                    lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, tx.getDescription(), 1));
                }
            }
        }

        save(company, tx.getTransactionDate(), tx.getDescription() != null ? tx.getDescription() : "İşlem",
                JournalSourceType.TRANSACTION, tx.getTransactionId(), lines);
    }

    public void reverseForTransaction(Long companyId, Long transactionId, String reason) {
        reverseForSource(companyId, JournalSourceType.TRANSACTION, transactionId, reason);
    }

    public Long createForCustomerOpening(Customer customer) {
        if (customer == null || customer.getCustomerId() == null || customer.getCompany() == null) {
            return null;
        }

        Long companyId = customer.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return null;
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.CUSTOMER_OPENING, customer.getCustomerId())) {
            return null;
        }

        BigDecimal openingBalance = nvl(customer.getOpeningBalance());
        if (openingBalance.compareTo(BigDecimal.ZERO) == 0) return null;

        Company company = customer.getCompany();
        BigDecimal amount = openingBalance.abs();
        String customerCode = resolveCustomerOpeningCode(companyId, customer, openingBalance);

        List<JournalEntryLine> lines = new ArrayList<>();
        if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, customerCode), amount, BigDecimal.ZERO,
                    "Cari acilis bakiyesi", 0));
            lines.add(line(company, accountId(companyId, "570"), BigDecimal.ZERO, amount,
                    "Cari acilis karsiligi", 1));
        } else {
            lines.add(line(company, accountId(companyId, "570"), amount, BigDecimal.ZERO,
                    "Cari acilis karsiligi", 0));
            lines.add(line(company, accountId(companyId, customerCode), BigDecimal.ZERO, amount,
                    "Cari acilis bakiyesi", 1));
        }

        LocalDate entryDate = customer.getOpeningBalanceDate() != null
                ? customer.getOpeningBalanceDate()
                : LocalDate.now();
        periodGuard.assertOpen(entryDate);
        JournalEntry entry = save(company, entryDate,
                "Cari acilis bakiyesi: " + customer.getName(),
                JournalSourceType.CUSTOMER_OPENING, customer.getCustomerId(), lines);
        return entry.getEntryId();
    }

    public void reverseForCustomerOpening(Long companyId, Long customerId, String reason) {
        reverseForSource(companyId, JournalSourceType.CUSTOMER_OPENING, customerId, reason);
    }

    // ─── Fixed asset depreciation hooks ───────────────────────────────────────

    public Long createForDepreciation(DepreciationLine depreciationLine) {
        FixedAsset asset = depreciationLine.getFixedAsset();
        Long companyId = asset.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return null;
        if (depreciationLine.getId() == null) {
            throw new BusinessException("Amortisman satırı kaydedilmeden muhasebe fişi oluşturulamaz.");
        }
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.DEPRECIATION, depreciationLine.getId())) {
            return depreciationLine.getJournalEntryId();
        }

        BigDecimal amount = nvl(depreciationLine.getDepreciationAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return null;

        Company company = asset.getCompany();
        AssetCategory category = asset.getCategory();
        String accumulatedCode = category != null && category.getDepreciationAccountCode() != null
                && !category.getDepreciationAccountCode().isBlank()
                ? category.getDepreciationAccountCode()
                : "257";
        String expenseCode = "770";

        List<JournalEntryLine> lines = new ArrayList<>();
        lines.add(line(company, accountId(companyId, expenseCode), amount, BigDecimal.ZERO,
                "Amortisman gideri: " + asset.getName(), 0));
        lines.add(line(company, accountId(companyId, accumulatedCode), BigDecimal.ZERO, amount,
                "Birikmiş amortisman: " + asset.getName(), 1));

        LocalDate entryDate = YearMonth
                .of(depreciationLine.getPeriodYear(), depreciationLine.getPeriodMonth())
                .atEndOfMonth();
        periodGuard.assertOpen(entryDate);
        JournalEntry entry = save(company, entryDate,
                "Amortisman: " + asset.getName() + " " + depreciationLine.getPeriodYear() + "/" + depreciationLine.getPeriodMonth(),
                JournalSourceType.DEPRECIATION, depreciationLine.getId(), lines);
        return entry.getEntryId();
    }

    // ─── Year-end closing ─────────────────────────────────────────────────────

    /**
     * Creates the year-end closing journal entry for the given company and year.
     * Closes 6xx income and 7xx cost/expense accounts into 590 (profit) or 591 (loss).
     * Returns the created JournalEntry ID.
     */
    public Long createForVatSettlement(VatPeriod vatPeriod) {
        if (vatPeriod == null || vatPeriod.getId() == null || vatPeriod.getCompany() == null) {
            return null;
        }

        Long companyId = vatPeriod.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return null;
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.VAT_DECLARATION, vatPeriod.getId())) {
            return vatPeriod.getSettlementJournalEntryId();
        }

        LocalDate entryDate = YearMonth.of(vatPeriod.getYear(), vatPeriod.getMonth()).atEndOfMonth();
        periodGuard.assertOpen(entryDate);

        BigDecimal outputVat = nvl(vatPeriod.getTotalOutputVat());
        BigDecimal inputVat = nvl(vatPeriod.getTotalInputVat());
        BigDecimal previousCarried = nvl(vatPeriod.getPreviousCarriedForwardVat());
        BigDecimal netPayable = nvl(vatPeriod.getNetPayableVat());
        BigDecimal newCarried = nvl(vatPeriod.getCarriedForwardVat());

        if (outputVat.compareTo(BigDecimal.ZERO) == 0
                && inputVat.compareTo(BigDecimal.ZERO) == 0
                && previousCarried.compareTo(BigDecimal.ZERO) == 0
                && netPayable.compareTo(BigDecimal.ZERO) == 0
                && newCarried.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        Company company = vatPeriod.getCompany();
        List<JournalEntryLine> lines = new ArrayList<>();
        int order = 0;

        if (outputVat.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, "391"), outputVat, BigDecimal.ZERO,
                    "Hesaplanan KDV mahsubu", order++));
        }
        if (newCarried.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, "190"), newCarried, BigDecimal.ZERO,
                    "Devreden KDV", order++));
        }
        if (inputVat.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, "191"), BigDecimal.ZERO, inputVat,
                    "Indirilecek KDV mahsubu", order++));
        }
        if (previousCarried.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, "190"), BigDecimal.ZERO, previousCarried,
                    "Onceki donem devreden KDV mahsubu", order++));
        }
        if (netPayable.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(line(company, accountId(companyId, "360"), BigDecimal.ZERO, netPayable,
                    "Odenecek KDV", order));
        }

        JournalEntry entry = save(company, entryDate,
                "KDV mahsubu: " + vatPeriod.getYear() + "/" + String.format("%02d", vatPeriod.getMonth()),
                JournalSourceType.VAT_DECLARATION, vatPeriod.getId(), lines);
        return entry.getEntryId();
    }

    public Long createYearEndClosingEntry(Long companyId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate   = LocalDate.of(year, 12, 31);

        List<Object[]> rows = lineRepository.sumByAccountForPeriod(companyId, startDate, endDate);

        Company company = companyRepository.getReferenceById(companyId);
        List<JournalEntryLine> lines = new ArrayList<>();
        int order = 0;

        BigDecimal totalIncome  = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Object[] row : rows) {
            Long accId  = (Long) row[0];
            BigDecimal debit  = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal credit = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            ChartOfAccount account = chartOfAccountService.findByAccountId(accId).orElse(null);
            if (account == null) continue;
            AccountType type = account.getAccountType();

            // Income accounts: credit balance closes with debit.
            if (type == AccountType.INCOME) {
                BigDecimal balance = credit.subtract(debit); // income has credit balance
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(line(company, accId, balance, BigDecimal.ZERO, "Yıl sonu kapanışı - gelir kapatma", order++));
                    totalIncome = totalIncome.add(balance);
                }
            }
            // Expense and cost accounts: debit balance closes with credit.
            if (type == AccountType.EXPENSE || type == AccountType.COST) {
                BigDecimal balance = debit.subtract(credit); // expenses have debit balance
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(line(company, accId, BigDecimal.ZERO, balance, "Yıl sonu kapanışı - gider kapatma", order++));
                    totalExpense = totalExpense.add(balance);
                }
            }
        }

        if (lines.isEmpty()) return null; // nothing to close

        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
            // Profit: credit 590
            lines.add(line(company, accountId(companyId, "590"), BigDecimal.ZERO, netProfit,
                    "Dönem net karı " + year, order));
        } else if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            // Loss: debit 591
            lines.add(line(company, accountId(companyId, "591"), netProfit.negate(), BigDecimal.ZERO,
                    "Dönem net zararı " + year, order));
        } else {
            return null; // zero balance, nothing to post
        }

        JournalEntry entry = save(company, endDate,
                year + " yılı kapanış fişi",
                JournalSourceType.MANUAL, null, lines);
        return entry.getEntryId();
    }

    // ─── Manual entry (controller-facing) ────────────────────────────────────

    public JournalEntryResponseDto createManualEntry(JournalEntryRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        periodGuard.assertOpen(dto.entryDate());
        if (!chartOfAccountService.isAccountingSetup(companyId)) {
            throw new BusinessException("Şirket için hesap planı henüz kurulmamış. Önce TDHP'yi yükleyin.");
        }
        Company company = companyRepository.getReferenceById(companyId);
        List<JournalEntryLine> lines = new ArrayList<>();
        int order = 0;
        for (var lineDto : dto.lines()) {
            lines.add(line(company, lineDto.accountId(), lineDto.debitAmount(), lineDto.creditAmount(), lineDto.description(), order++));
        }
        validateBalance(lines);
        JournalEntry entry = save(company, dto.entryDate(), dto.description(),
                JournalSourceType.MANUAL, null, lines);
        return toResponseDto(entry);
    }

    public JournalEntryResponseDto reverseEntry(Long entryId, String reason) {
        Long companyId = companyContext.getCurrentCompanyId();
        JournalEntry original = entryRepository
                .findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(companyId, entryId)
                .orElseThrow(() -> new BusinessException("Fiş bulunamadı: " + entryId));
        if (original.isReversed()) throw new BusinessException("Bu fiş zaten ters çevrilmiş");
        JournalEntry reversal = reverseEntryInternal(original, reason);
        return toResponseDto(reversal);
    }

    public void delete(Long entryId) {
        Long companyId = companyContext.getCurrentCompanyId();
        JournalEntry entry = entryRepository
                .findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(companyId, entryId)
                .orElseThrow(() -> new BusinessException("Fiş bulunamadı: " + entryId));
        if (entry.getSourceType() != JournalSourceType.MANUAL) {
            throw new BusinessException("Sadece manuel fişler silinebilir");
        }
        periodGuard.assertOpen(entry.getEntryDate());
        entry.setDeleted(true);
        entry.setDeletedAt(LocalDateTime.now());
        entryRepository.save(entry);
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    public JournalEntryResponseDto getById(Long entryId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(entryRepository
                .findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(companyId, entryId)
                .orElseThrow(() -> new BusinessException("Fiş bulunamadı: " + entryId)));
    }

    public Page<JournalEntryResponseDto> list(Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();
        return entryRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByEntryDateDescEntryIdDesc(companyId, pageable)
                .map(this::toResponseDto);
    }

    // ─── Mizan (Trial Balance) ────────────────────────────────────────────────

    public List<TrialBalanceRowDto> getTrialBalance(LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toTrialBalanceRows(lineRepository.sumByAccountForPeriod(companyId, startDate, endDate));
    }

    public IncomeStatementResponseDto getIncomeStatement(LocalDate startDate, LocalDate endDate) {
        List<TrialBalanceRowDto> rows = getTrialBalance(startDate, endDate);

        List<FinancialStatementLineDto> incomeLines = rows.stream()
                .filter(r -> r.accountType() == AccountType.INCOME)
                .map(r -> toStatementLine(r, r.totalCredit().subtract(r.totalDebit())))
                .filter(l -> l.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                .toList();

        List<FinancialStatementLineDto> expenseLines = rows.stream()
                .filter(r -> r.accountType() == AccountType.EXPENSE || r.accountType() == AccountType.COST)
                .map(r -> toStatementLine(r, r.totalDebit().subtract(r.totalCredit())))
                .filter(l -> l.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                .toList();

        BigDecimal totalIncome = sumStatementLines(incomeLines);
        BigDecimal totalExpense = sumStatementLines(expenseLines);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new IncomeStatementResponseDto(
                startDate,
                endDate,
                totalIncome,
                totalExpense,
                netProfit,
                margin,
                List.of(
                        section("6", "Gelirler", incomeLines),
                        section("6/7", "Giderler ve Maliyetler", expenseLines)
                )
        );
    }

    public BalanceSheetResponseDto getBalanceSheet(LocalDate asOfDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<TrialBalanceRowDto> rows = toTrialBalanceRows(lineRepository.sumByAccountUntil(companyId, asOfDate));

        List<FinancialStatementLineDto> assetLines = rows.stream()
                .filter(r -> r.accountType() == AccountType.ASSET)
                .map(r -> toStatementLine(r, r.totalDebit().subtract(r.totalCredit())))
                .filter(l -> l.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                .toList();

        List<FinancialStatementLineDto> liabilityLines = rows.stream()
                .filter(r -> r.accountType() == AccountType.LIABILITY)
                .map(r -> toStatementLine(r, r.totalCredit().subtract(r.totalDebit())))
                .filter(l -> l.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                .toList();

        List<FinancialStatementLineDto> equityLines = rows.stream()
                .filter(r -> r.accountType() == AccountType.EQUITY)
                .map(r -> toStatementLine(r, r.totalCredit().subtract(r.totalDebit())))
                .filter(l -> l.amount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                .toList();

        BigDecimal currentPeriodProfit = calculateCurrentPeriodProfit(companyId, asOfDate);
        if (currentPeriodProfit.compareTo(BigDecimal.ZERO) != 0) {
            List<FinancialStatementLineDto> equityWithProfit = new ArrayList<>(equityLines);
            equityWithProfit.add(new FinancialStatementLineDto(
                    null,
                    currentPeriodProfit.compareTo(BigDecimal.ZERO) > 0 ? "590" : "591",
                    "Donem Net Kar/Zarari",
                    AccountType.EQUITY,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    currentPeriodProfit
            ));
            equityLines = equityWithProfit.stream()
                    .sorted(Comparator.comparing(FinancialStatementLineDto::accountCode))
                    .toList();
        }

        BigDecimal totalAssets = sumStatementLines(assetLines);
        BigDecimal totalLiabilities = sumStatementLines(liabilityLines);
        BigDecimal totalEquity = sumStatementLines(equityLines);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        return new BalanceSheetResponseDto(
                asOfDate,
                totalAssets,
                totalLiabilities,
                totalEquity,
                totalLiabilitiesAndEquity,
                totalAssets.subtract(totalLiabilitiesAndEquity),
                List.of(
                        sectionByPrefix("1", "Donen Varliklar", "1", assetLines),
                        sectionByPrefix("2", "Duran Varliklar", "2", assetLines)
                ),
                List.of(
                        sectionByPrefix("3", "Kisa Vadeli Yabanci Kaynaklar", "3", liabilityLines),
                        sectionByPrefix("4", "Uzun Vadeli Yabanci Kaynaklar", "4", liabilityLines)
                ),
                List.of(sectionByPrefix("5", "Ozkaynaklar", "5", equityLines))
        );
    }

    // ─── Defter-i Kebir (General Ledger) ─────────────────────────────────────

    public List<GeneralLedgerRowDto> getGeneralLedger(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<JournalEntryLine> lines = lineRepository.findForGeneralLedger(companyId, accountId, startDate, endDate);
        BigDecimal running = BigDecimal.ZERO;
        List<GeneralLedgerRowDto> result = new ArrayList<>();
        for (JournalEntryLine l : lines) {
            running = running.add(l.getDebitAmount()).subtract(l.getCreditAmount());
            JournalEntry entry = l.getEntry();
            result.add(new GeneralLedgerRowDto(
                    l.getLineId(),
                    entry.getEntryDate(),
                    entry.getEntryNumber(),
                    l.getDescription(),
                    l.getDebitAmount(),
                    l.getCreditAmount(),
                    running));
        }
        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private List<TrialBalanceRowDto> toTrialBalanceRows(List<Object[]> rows) {
        if (rows == null) return List.of();
        return rows.stream().map(row -> {
            Long accId = (Long) row[0];
            BigDecimal debit = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal credit = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            ChartOfAccount account = chartOfAccountService.findByAccountId(accId).orElse(null);
            String code = account != null ? account.getAccountCode() : "";
            String name = account != null ? account.getAccountName() : "";
            AccountType type = account != null ? account.getAccountType() : null;
            return new TrialBalanceRowDto(accId, code, name, type, debit, credit, debit.subtract(credit));
        }).sorted(Comparator.comparing(TrialBalanceRowDto::accountCode)).toList();
    }

    private FinancialStatementLineDto toStatementLine(TrialBalanceRowDto row, BigDecimal amount) {
        return new FinancialStatementLineDto(
                row.accountId(),
                row.accountCode(),
                row.accountName(),
                row.accountType(),
                row.totalDebit(),
                row.totalCredit(),
                amount != null ? amount : BigDecimal.ZERO
        );
    }

    private FinancialStatementSectionDto section(String code, String name, List<FinancialStatementLineDto> lines) {
        return new FinancialStatementSectionDto(code, name, sumStatementLines(lines), lines);
    }

    private FinancialStatementSectionDto sectionByPrefix(
            String code,
            String name,
            String prefix,
            List<FinancialStatementLineDto> sourceLines) {
        List<FinancialStatementLineDto> lines = sourceLines.stream()
                .filter(l -> l.accountCode() != null && l.accountCode().startsWith(prefix))
                .toList();
        return section(code, name, lines);
    }

    private BigDecimal sumStatementLines(List<FinancialStatementLineDto> lines) {
        return lines.stream()
                .map(FinancialStatementLineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCurrentPeriodProfit(Long companyId, LocalDate asOfDate) {
        LocalDate yearStart = LocalDate.of(asOfDate.getYear(), 1, 1);
        List<TrialBalanceRowDto> rows = toTrialBalanceRows(
                lineRepository.sumByAccountForPeriod(companyId, yearStart, asOfDate));

        BigDecimal income = rows.stream()
                .filter(r -> r.accountType() == AccountType.INCOME)
                .map(r -> r.totalCredit().subtract(r.totalDebit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = rows.stream()
                .filter(r -> r.accountType() == AccountType.EXPENSE || r.accountType() == AccountType.COST)
                .map(r -> r.totalDebit().subtract(r.totalCredit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return income.subtract(expense);
    }

    private JournalEntry save(Company company, LocalDate date, String description,
                               JournalSourceType sourceType, Long sourceId,
                               List<JournalEntryLine> lines) {
        validateBalance(lines);
        JournalEntry entry = new JournalEntry();
        entry.setCompany(company);
        entry.setEntryNumber(nextEntryNumber(company.getCompanyId()));
        entry.setEntryDate(date);
        entry.setDescription(description);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        lines.forEach(l -> l.setEntry(entry));
        entry.setLines(lines);
        return entryRepository.save(entry);
    }

    private void reverseForSource(Long companyId, JournalSourceType sourceType, Long sourceId, String reason) {
        entryRepository
                .findByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                        companyId, sourceType, sourceId)
                .ifPresent(entry -> reverseEntryInternal(entry, reason));
    }

    private JournalEntry reverseEntryInternal(JournalEntry original, String reason) {
        LocalDate reversalDate = LocalDate.now();
        periodGuard.assertOpen(original.getEntryDate());
        periodGuard.assertOpen(reversalDate);
        Company company = original.getCompany();
        List<JournalEntryLine> reversalLines = new ArrayList<>();
        int order = 0;
        for (JournalEntryLine l : original.getLines()) {
            reversalLines.add(line(company, l.getAccountId(), l.getCreditAmount(), l.getDebitAmount(),
                    l.getDescription(), order++));
        }
        String desc = "İPTAL: " + original.getEntryNumber() + (reason != null ? " - " + reason : "");
        JournalEntry reversal = save(company, reversalDate, desc,
                JournalSourceType.REVERSAL, original.getEntryId(), reversalLines);
        original.setReversed(true);
        original.setReversedEntryId(reversal.getEntryId());
        entryRepository.save(original);
        return reversal;
    }

    private String nextEntryNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        JournalEntrySequence seq = sequenceRepository
                .findByCompanyIdAndYearForUpdate(companyId, year)
                .orElseGet(() -> {
                    JournalEntrySequence s = new JournalEntrySequence();
                    s.setCompanyId(companyId);
                    s.setYear(year);
                    s.setLastSeq(0);
                    return s;
                });
        seq.setLastSeq(seq.getLastSeq() + 1);
        sequenceRepository.save(seq);
        return String.format("FIS-%d-%04d", year, seq.getLastSeq());
    }

    private void validateBalance(List<JournalEntryLine> lines) {
        BigDecimal totalDebit = lines.stream().map(JournalEntryLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalEntryLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(
                    String.format("Fiş dengesi bozuk: borç=%.2f, alacak=%.2f", totalDebit, totalCredit));
        }
    }

    private JournalEntryLine line(Company company, Long accId, BigDecimal debit, BigDecimal credit,
                                   String description, int order) {
        JournalEntryLine l = new JournalEntryLine();
        l.setCompany(company);
        l.setAccountId(accId);
        l.setDebitAmount(debit != null ? debit : BigDecimal.ZERO);
        l.setCreditAmount(credit != null ? credit : BigDecimal.ZERO);
        l.setDescription(description);
        l.setLineOrder(order);
        return l;
    }

    // Looks up accountId by code; falls back to parent prefix (first 3 chars) if exact code not found
    private Long accountId(Long companyId, String code) {
        return chartOfAccountService.findByCode(companyId, code)
                .map(ChartOfAccount::getAccountId)
                .orElseGet(() -> {
                    // Try parent (first segment)
                    String parent = code.length() >= 3 ? code.substring(0, 3) : code;
                    return chartOfAccountService.findByCode(companyId, parent)
                            .map(ChartOfAccount::getAccountId)
                            .orElseThrow(() -> new BusinessException(
                                    "Hesap planında hesap bulunamadı: " + code +
                                    " — Şirket için TDHP kurulumunu kontrol edin."));
                });
    }

    private String resolveCustomerCode(Long companyId, Long customerId, boolean isVendor) {
        if (customerId == null) return isVendor ? "320" : "120";
        return customerRepository.findById(customerId)
                .map(c -> {
                    String code = c.getAccountCode();
                    if (code != null && !code.isBlank() &&
                            chartOfAccountService.findByCode(companyId, code).isPresent()) {
                        return code;
                    }
                    return isVendor ? "320" : "120";
                })
                .orElse(isVendor ? "320" : "120");
    }

    private String resolveCustomerOpeningCode(Long companyId, Customer customer, BigDecimal openingBalance) {
        String code = customer.getAccountCode();
        if (code != null && !code.isBlank() &&
                chartOfAccountService.findByCode(companyId, code).isPresent()) {
            return code;
        }

        boolean vendorAccount = openingBalance.compareTo(BigDecimal.ZERO) < 0
                || customer.getCustomerRole() == CustomerRole.SELLER;
        return vendorAccount ? "320" : "120";
    }

    private String resolveBankAccountCode(Long companyId, Long bankAccountId) {
        if (bankAccountId == null) return "102";
        return bankAccountRepository.findById(bankAccountId)
                .map(b -> {
                    String fallback = switch (b.getAccountType() != null ? b.getAccountType() : BankAccountType.BANK) {
                        case CASH -> "100";
                        case POS -> "108";
                        case BANK -> "102";
                    };
                    String code = b.getAccountCode();
                    if (code != null && !code.isBlank() &&
                            chartOfAccountService.findByCode(companyId, code).isPresent()) {
                        return code;
                    }
                    return fallback;
                })
                .orElse("102");
    }

    private String resolveChequeAccountCode(Cheque cheque) {
        ChequeType type = cheque.getChequeType() != null ? cheque.getChequeType() : ChequeType.RECEIVABLE;
        ChequeKind kind = cheque.getChequeKind() != null ? cheque.getChequeKind() : ChequeKind.CHEQUE;
        boolean receivable = type == ChequeType.RECEIVABLE;
        if (kind == ChequeKind.PROMISSORY_NOTE) {
            return receivable ? "121" : "321";
        }
        return receivable ? "101" : "103";
    }

    private BigDecimal calculateInvoiceStockCost(Long companyId, Long invoiceId) {
        if (invoiceId == null) return BigDecimal.ZERO;
        return invoiceLineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId)
                .stream()
                .map(line -> productCost(companyId, line).multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal productCost(Long companyId, InvoiceLineItem line) {
        if (line.getProductId() == null) return BigDecimal.ZERO;
        return productRepository
                .findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(line.getProductId(), companyId)
                .map(Product::getCostPrice)
                .filter(cost -> cost.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public JournalEntryResponseDto toResponseDto(JournalEntry e) {
        List<JournalEntryLineResponseDto> lineDtos = e.getLines().stream()
                .map(l -> {
                    String code = l.getAccount() != null ? l.getAccount().getAccountCode() : null;
                    String name = l.getAccount() != null ? l.getAccount().getAccountName() : null;
                    return new JournalEntryLineResponseDto(
                            l.getLineId(), l.getAccountId(), code, name,
                            l.getDebitAmount(), l.getCreditAmount(), l.getDescription(), l.getLineOrder());
                }).toList();
        return new JournalEntryResponseDto(
                e.getEntryId(), e.getEntryNumber(), e.getEntryDate(), e.getDescription(),
                e.getSourceType(), e.getSourceId(), e.isReversed(), e.getReversedEntryId(),
                lineDtos, e.getCreatedAt());
    }
}
