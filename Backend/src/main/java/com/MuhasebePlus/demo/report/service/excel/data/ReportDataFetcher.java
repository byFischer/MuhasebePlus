package com.MuhasebePlus.demo.report.service.excel.data;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportDataFetcher {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public List<Invoice> fetchPaidInvoices(Long companyId, InvoiceType type, LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return invoiceRepository
                .findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                        PaymentStatus.paid, type, companyId)
                .stream()
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().isBefore(startDt)
                        && !i.getCreatedAt().isAfter(endDt))
                .collect(Collectors.toList());
    }

    public List<Transaction> fetchTransactions(Long companyId, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository
                .findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                        start, end, companyId)
                .stream()
                .filter(t -> t.getTransactionType() == type)
                .collect(Collectors.toList());
    }

    public Map<Integer, Product> loadProductMap(Long companyId, List<Stock> stocks) {
        if (stocks.isEmpty()) return Map.of();
        List<Integer> ids = stocks.stream().map(Stock::getProductId).distinct().toList();
        return productRepository.findByProductIdInAndCompanyCompanyId(ids, companyId)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
    }

    public String customerName(Long customerId) {
        if (customerId == null) return "";
        return customerRepository.findById(customerId).map(Customer::getName).orElse("Müşteri #" + customerId);
    }
}
