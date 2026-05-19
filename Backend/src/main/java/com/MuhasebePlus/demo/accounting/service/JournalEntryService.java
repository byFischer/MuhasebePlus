package com.MuhasebePlus.demo.accounting.service;

import com.MuhasebePlus.demo.accounting.dto.request.JournalEntryRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.GeneralLedgerRowDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryLineResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.dto.response.TrialBalanceRowDto;
import com.MuhasebePlus.demo.accounting.entity.*;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryLineRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntryRepository;
import com.MuhasebePlus.demo.accounting.repository.JournalEntrySequenceRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final CompanyContext companyContext;

    // ─── Invoice hooks ───────────────────────────────────────────────────────

    public void createForInvoice(Invoice invoice) {
        Long companyId = invoice.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
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

    // ─── Transaction hooks ────────────────────────────────────────────────────

    public void createForTransaction(Transaction tx) {
        Long companyId = tx.getCompany().getCompanyId();
        if (!chartOfAccountService.isAccountingSetup(companyId)) return;
        if (entryRepository.existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                companyId, JournalSourceType.TRANSACTION, tx.getTransactionId())) return;

        BigDecimal amount = tx.getAmount();
        Company company = tx.getCompany();
        String bankCode = resolveBankAccountCode(companyId, tx.getAccountId());

        List<JournalEntryLine> lines = new ArrayList<>();
        if (tx.getTransactionType() == TransactionType.INCOME) {
            // Debit: Bank/Cash, Credit: Other income (602)
            lines.add(line(company, accountId(companyId, bankCode), amount, BigDecimal.ZERO, tx.getDescription(), 0));
            lines.add(line(company, accountId(companyId, "602"), BigDecimal.ZERO, amount, tx.getDescription(), 1));
        } else {
            // Debit: Expense (632 general admin), Credit: Bank/Cash
            lines.add(line(company, accountId(companyId, "632"), amount, BigDecimal.ZERO, tx.getDescription(), 0));
            lines.add(line(company, accountId(companyId, bankCode), BigDecimal.ZERO, amount, tx.getDescription(), 1));
        }

        save(company, tx.getTransactionDate(), tx.getDescription() != null ? tx.getDescription() : "İşlem",
                JournalSourceType.TRANSACTION, tx.getTransactionId(), lines);
    }

    public void reverseForTransaction(Long companyId, Long transactionId, String reason) {
        reverseForSource(companyId, JournalSourceType.TRANSACTION, transactionId, reason);
    }

    // ─── Manual entry (controller-facing) ────────────────────────────────────

    public JournalEntryResponseDto createManualEntry(JournalEntryRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
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
        List<Object[]> rows = lineRepository.sumByAccountForPeriod(companyId, startDate, endDate);
        return rows.stream().map(row -> {
            Long accId = (Long) row[0];
            BigDecimal debit = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal credit = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            ChartOfAccount account = chartOfAccountService.findByAccountId(accId).orElse(null);
            String code = account != null ? account.getAccountCode() : "";
            String name = account != null ? account.getAccountName() : "";
            AccountType type = account != null ? account.getAccountType() : null;
            return new TrialBalanceRowDto(accId, code, name, type, debit, credit, debit.subtract(credit));
        }).sorted((a, b) -> a.accountCode().compareTo(b.accountCode())).toList();
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
        Company company = original.getCompany();
        List<JournalEntryLine> reversalLines = new ArrayList<>();
        int order = 0;
        for (JournalEntryLine l : original.getLines()) {
            reversalLines.add(line(company, l.getAccountId(), l.getCreditAmount(), l.getDebitAmount(),
                    l.getDescription(), order++));
        }
        String desc = "İPTAL: " + original.getEntryNumber() + (reason != null ? " - " + reason : "");
        JournalEntry reversal = save(company, LocalDate.now(), desc,
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

    private String resolveBankAccountCode(Long companyId, Long bankAccountId) {
        if (bankAccountId == null) return "102";
        return bankAccountRepository.findById(bankAccountId)
                .map(b -> {
                    String code = b.getAccountCode();
                    if (code != null && !code.isBlank() &&
                            chartOfAccountService.findByCode(companyId, code).isPresent()) {
                        return code;
                    }
                    return "102";
                })
                .orElse("102");
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
