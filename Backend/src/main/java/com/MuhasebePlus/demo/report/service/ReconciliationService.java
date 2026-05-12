package com.MuhasebePlus.demo.report.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.dto.response.ReconciliationItemDto;
import com.MuhasebePlus.demo.report.dto.response.ReconciliationResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    public ReconciliationResponseDto generateReconciliation(Long customerId, LocalDate periodStart, LocalDate periodEnd) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .stream()
            .filter(i -> !i.isCancelled() && i.getInvoiceType() == InvoiceType.sale)
            .filter(i -> !i.getInvoiceDate().isBefore(periodStart) && !i.getInvoiceDate().isAfter(periodEnd))
            .toList();

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        List<ReconciliationItemDto> openItems = new ArrayList<>();

        for (Invoice inv : invoices) {
            totalInvoiced = totalInvoiced.add(inv.getTotalAmount());

            BigDecimal paid = paymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId)
                .stream()
                .map(InvoicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalCollected = totalCollected.add(paid);

            BigDecimal remaining = inv.getTotalAmount().subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                long daysOverdue = inv.getDueDate() != null && inv.getDueDate().isBefore(LocalDate.now())
                    ? ChronoUnit.DAYS.between(inv.getDueDate(), LocalDate.now()) : 0;
                openItems.add(new ReconciliationItemDto(
                    inv.getInvoiceNumber(), inv.getInvoiceDate(), inv.getDueDate(),
                    inv.getTotalAmount(), paid, remaining, daysOverdue));
            }
        }

        return new ReconciliationResponseDto(
            customerId, customer.getName(), customer.getTaxNumber(),
            company.getCompanyName(),
            company.getTaxNumber() != null ? company.getTaxNumber() : "",
            periodStart, periodEnd,
            BigDecimal.ZERO, totalInvoiced, totalCollected,
            totalInvoiced.subtract(totalCollected), openItems
        );
    }
}
