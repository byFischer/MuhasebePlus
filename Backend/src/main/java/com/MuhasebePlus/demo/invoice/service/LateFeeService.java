package com.MuhasebePlus.demo.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.LateFeeConfig;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.repository.LateFeeConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LateFeeService {

    private final LateFeeConfigRepository lateFeeConfigRepository;
    private final InvoiceRepository invoiceRepository;

    public BigDecimal calculateLateFee(Long invoiceId, Long companyId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            return BigDecimal.ZERO;
        }
        if (invoice.getDueDate() == null || invoice.getDueDate().isAfter(LocalDate.now())) {
            return BigDecimal.ZERO;
        }

        LateFeeConfig config = lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
            .orElse(null);
        if (config == null) return BigDecimal.ZERO;

        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
        long graceDays = config.getGracePeriodDays();
        if (daysOverdue <= graceDays) return BigDecimal.ZERO;

        long overdueDays = daysOverdue - graceDays;
        long fullMonths = overdueDays / 30;
        if (overdueDays % 30 > 0) fullMonths++;

        BigDecimal monthlyRate = config.getMonthlyRate();
        BigDecimal remainingAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;

        return remainingAmount
            .multiply(monthlyRate)
            .multiply(BigDecimal.valueOf(fullMonths))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public LateFeeConfig getConfig(Long companyId) {
        return lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
            .orElseGet(() -> LateFeeConfig.builder()
                .monthlyRate(BigDecimal.valueOf(2.00))
                .gracePeriodDays(7)
                .isActive(true)
                .build());
    }

    public LateFeeConfig saveConfig(Long companyId, BigDecimal monthlyRate, Integer gracePeriodDays) {
        LateFeeConfig config = lateFeeConfigRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
            .orElse(null);
        if (config == null) {
            config = new LateFeeConfig();
            config.setCompany(new com.MuhasebePlus.demo.company.entity.Company());
            config.getCompany().setCompanyId(companyId);
        }
        config.setMonthlyRate(monthlyRate);
        config.setGracePeriodDays(gracePeriodDays);
        return lateFeeConfigRepository.save(config);
    }
}
