package com.MuhasebePlus.demo.report.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.dto.response.StatementLineDto;
import com.MuhasebePlus.demo.report.dto.response.StatementResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CompanyContext companyContext;

    public StatementResponseDto generateStatement(Long customerId, LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId);
        BigDecimal openingBalance = calculateOpeningBalance(customer, invoices, companyId, startDate);

        List<StatementEvent> events = new ArrayList<>();
        addOpeningEvent(customer, startDate, endDate, events);
        for (Invoice inv : invoices) {
            if (!isAccountingInvoice(inv)) continue;
            boolean isSale = inv.getInvoiceType() == InvoiceType.sale;
            BigDecimal invoiceAmount = nvl(inv.getTotalAmount());

            if (isInRange(inv.getInvoiceDate(), startDate, endDate)) {
                events.add(new StatementEvent(
                    inv.getInvoiceDate(), 1,
                    isSale ? "Satis Faturasi" : "Alis Faturasi",
                    inv.getInvoiceNumber(),
                    isSale ? invoiceAmount : BigDecimal.ZERO,
                    isSale ? BigDecimal.ZERO : invoiceAmount
                ));
            }

            if (inv.getInvoiceId() != null) {
                List<InvoicePayment> payments = paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
                for (InvoicePayment pmt : payments) {
                    if (!isInRange(pmt.getPaymentDate(), startDate, endDate)) continue;
                    BigDecimal paymentAmount = nvl(pmt.getAmount());
                    events.add(new StatementEvent(
                        pmt.getPaymentDate(), 2,
                        isSale ? "Tahsilat" : "Odeme",
                        "#PMT" + pmt.getPaymentId(),
                        isSale ? BigDecimal.ZERO : paymentAmount,
                        isSale ? paymentAmount : BigDecimal.ZERO
                    ));
                }
            }
        }

        BigDecimal runningBalance = openingBalance;
        List<StatementLineDto> lines = new ArrayList<>();
        List<StatementEvent> sortedEvents = events.stream()
            .sorted(Comparator
                .comparing(StatementEvent::date)
                .thenComparing(StatementEvent::order)
                .thenComparing(e -> e.documentNumber() != null ? e.documentNumber() : ""))
            .toList();

        for (StatementEvent event : sortedEvents) {
            runningBalance = runningBalance.add(event.debit()).subtract(event.credit());
            lines.add(new StatementLineDto(event.date(), event.description(), event.documentNumber(),
                    event.debit(), event.credit(), runningBalance));
        }

        BigDecimal totalDebit = lines.stream().map(StatementLineDto::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(StatementLineDto::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatementResponseDto(
            customerId, customer.getName(), customer.getTaxNumber(), customer.getAccountCode(),
            startDate, endDate, openingBalance, runningBalance, totalDebit, totalCredit, lines
        );
    }

    private StatementResponseDto generateStatementLegacy(Long customerId, LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        BigDecimal openingBalance = calculateOpeningBalance(customerId, companyId, startDate);

        List<StatementLineDto> lines = new ArrayList<>();
        BigDecimal runningBalance = openingBalance;

        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .stream()
            .filter(i -> !i.getInvoiceDate().isBefore(startDate) && !i.getInvoiceDate().isAfter(endDate))
            .sorted(Comparator.comparing(Invoice::getInvoiceDate))
            .toList();

        for (Invoice inv : invoices) {
            boolean isSale = inv.getInvoiceType() == InvoiceType.sale;
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (isSale) {
                debit = inv.getTotalAmount();
                runningBalance = runningBalance.add(debit);
            } else {
                credit = inv.getTotalAmount();
                runningBalance = runningBalance.subtract(credit);
            }

            String desc = (isSale ? "Satış Faturası" : "Alış Faturası") + (inv.isCancelled() ? " (İPTAL)" : "");
            lines.add(new StatementLineDto(inv.getInvoiceDate(), desc, inv.getInvoiceNumber(), debit, credit, runningBalance));

            List<InvoicePayment> payments = paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
            for (InvoicePayment pmt : payments) {
                runningBalance = runningBalance.subtract(pmt.getAmount());
                lines.add(new StatementLineDto(pmt.getPaymentDate(), "Tahsilat", "#PMT" + pmt.getPaymentId(),
                    BigDecimal.ZERO, pmt.getAmount(), runningBalance));
            }
        }

        lines.sort(Comparator.comparing(StatementLineDto::date));

        BigDecimal totalDebit = lines.stream().map(StatementLineDto::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(StatementLineDto::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatementResponseDto(
            customerId, customer.getName(), customer.getTaxNumber(), customer.getAccountCode(),
            startDate, endDate, openingBalance, runningBalance, totalDebit, totalCredit, lines
        );
    }

    private BigDecimal calculateOpeningBalance(Customer customer, List<Invoice> invoices, Long companyId, LocalDate beforeDate) {
        BigDecimal balance = BigDecimal.ZERO;
        if (beforeDate == null) {
            return nvl(customer.getOpeningBalance());
        }

        LocalDate openingDate = customer.getOpeningBalanceDate();
        BigDecimal customerOpening = nvl(customer.getOpeningBalance());
        if (customerOpening.compareTo(BigDecimal.ZERO) != 0
                && (openingDate == null || openingDate.isBefore(beforeDate))) {
            balance = balance.add(customerOpening);
        }

        for (Invoice inv : invoices) {
            if (!isAccountingInvoice(inv)) continue;
            boolean isSale = inv.getInvoiceType() == InvoiceType.sale;
            BigDecimal invoiceAmount = nvl(inv.getTotalAmount());
            if (inv.getInvoiceDate() != null && inv.getInvoiceDate().isBefore(beforeDate)) {
                balance = isSale ? balance.add(invoiceAmount) : balance.subtract(invoiceAmount);
            }

            if (inv.getInvoiceId() != null) {
                List<InvoicePayment> payments = paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
                for (InvoicePayment pmt : payments) {
                    if (pmt.getPaymentDate() == null || !pmt.getPaymentDate().isBefore(beforeDate)) continue;
                    BigDecimal paymentAmount = nvl(pmt.getAmount());
                    balance = isSale ? balance.subtract(paymentAmount) : balance.add(paymentAmount);
                }
            }
        }
        return balance;
    }

    private void addOpeningEvent(Customer customer, LocalDate startDate, LocalDate endDate, List<StatementEvent> events) {
        BigDecimal customerOpening = nvl(customer.getOpeningBalance());
        LocalDate openingDate = customer.getOpeningBalanceDate();
        if (customerOpening.compareTo(BigDecimal.ZERO) == 0 || openingDate == null
                || !isInRange(openingDate, startDate, endDate)) {
            return;
        }

        events.add(new StatementEvent(
                openingDate, 0, "Acilis Bakiyesi", "-",
                customerOpening.compareTo(BigDecimal.ZERO) > 0 ? customerOpening : BigDecimal.ZERO,
                customerOpening.compareTo(BigDecimal.ZERO) < 0 ? customerOpening.abs() : BigDecimal.ZERO
        ));
    }

    private boolean isInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date != null
                && (startDate == null || !date.isBefore(startDate))
                && (endDate == null || !date.isAfter(endDate));
    }

    private boolean isAccountingInvoice(Invoice invoice) {
        return invoice != null
                && !invoice.isCancelled()
                && invoice.getPaymentStatus() != PaymentStatus.draft;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record StatementEvent(LocalDate date, int order, String description, String documentNumber,
                                  BigDecimal debit, BigDecimal credit) {
    }

    private BigDecimal calculateOpeningBalance(Long customerId, Long companyId, LocalDate beforeDate) {
        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .stream()
            .filter(i -> i.getInvoiceDate().isBefore(beforeDate))
            .toList();

        BigDecimal balance = BigDecimal.ZERO;
        for (Invoice inv : invoices) {
            if (inv.getInvoiceType() == InvoiceType.sale) {
                balance = balance.add(inv.getTotalAmount());
            } else {
                balance = balance.subtract(inv.getTotalAmount());
            }
            List<InvoicePayment> payments = paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
            for (InvoicePayment pmt : payments) {
                if (!pmt.getPaymentDate().isAfter(beforeDate)) {
                    balance = balance.subtract(pmt.getAmount());
                }
            }
        }
        return balance;
    }
}
