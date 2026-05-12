package com.MuhasebePlus.demo.common.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OverdueScheduler {

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueInvoices() {
        List<Long> companyIds = companyRepository.findAll().stream()
            .map(c -> c.getCompanyId())
            .distinct()
            .toList();

        LocalDate today = LocalDate.now();
        int totalUpdated = 0;

        for (Long companyId : companyIds) {
            List<Invoice> overdueInvoices = invoiceRepository
                .findByDueDateBeforeAndPaymentStatusInAndCompanyCompanyIdAndIsDeletedFalse(
                    today,
                    List.of(PaymentStatus.pending, PaymentStatus.partially_paid),
                    companyId
                );

            for (Invoice inv : overdueInvoices) {
                inv.setPaymentStatus(PaymentStatus.overdue);
                invoiceRepository.save(inv);
                totalUpdated++;
            }
        }

        if (totalUpdated > 0) {
            log.info("Marked {} invoice(s) as overdue", totalUpdated);
        }
    }
}
