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
