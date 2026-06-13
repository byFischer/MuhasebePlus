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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDataFetcherTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;

    @Test
    void fetchPaidInvoices_filtersByCreatedAtInsideRequestedDates() {
        ReportDataFetcher fetcher = fetcher();
        Invoice before = invoice(LocalDateTime.of(2025, 12, 31, 23, 59));
        Invoice inside = invoice(LocalDateTime.of(2026, 1, 15, 9, 0));
        Invoice endOfDay = invoice(LocalDateTime.of(2026, 1, 31, 23, 59, 59));
        Invoice after = invoice(LocalDateTime.of(2026, 2, 1, 0, 0));
        Invoice noDate = invoice(null);
        when(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.paid,
                InvoiceType.sale,
                1L
        )).thenReturn(List.of(before, inside, endOfDay, after, noDate));

        List<Invoice> result = fetcher.fetchPaidInvoices(
                1L,
                InvoiceType.sale,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(result).containsExactly(inside, endOfDay);
    }

    @Test
    void fetchTransactions_filtersByTransactionType() {
        ReportDataFetcher fetcher = fetcher();
        Transaction income = transaction(TransactionType.INCOME);
        Transaction expense = transaction(TransactionType.EXPENSE);
        when(transactionRepository.findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                1L
        )).thenReturn(List.of(income, expense));

        List<Transaction> result = fetcher.fetchTransactions(
                1L,
                TransactionType.INCOME,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(result).containsExactly(income);
    }

    @Test
    void loadProductMap_whenStocksEmpty_doesNotQueryProducts() {
        ReportDataFetcher fetcher = fetcher();

        Map<Integer, Product> result = fetcher.loadProductMap(1L, List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(productRepository);
    }

    @Test
    void loadProductMap_returnsDistinctProductsById() {
        ReportDataFetcher fetcher = fetcher();
        Product first = product(10, "Urun 10");
        Product second = product(20, "Urun 20");
        when(productRepository.findByProductIdInAndCompanyCompanyId(List.of(10, 20), 1L))
                .thenReturn(List.of(first, second));

        Map<Integer, Product> result = fetcher.loadProductMap(
                1L,
                List.of(stock(10), stock(10), stock(20))
        );

        assertThat(result).containsEntry(10, first).containsEntry(20, second);
    }

    @Test
    void customerName_handlesNullFoundAndMissingCustomer() {
        ReportDataFetcher fetcher = fetcher();
        Customer customer = new Customer();
        customer.setCustomerId(7L);
        customer.setName("Ada Ltd.");
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(customerRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(fetcher.customerName(null)).isEmpty();
        assertThat(fetcher.customerName(7L)).isEqualTo("Ada Ltd.");
        assertThat(fetcher.customerName(8L)).contains("8");
    }

    private ReportDataFetcher fetcher() {
        return new ReportDataFetcher(
                invoiceRepository,
                transactionRepository,
                customerRepository,
                productRepository
        );
    }

    private Invoice invoice(LocalDateTime createdAt) {
        Invoice invoice = new Invoice();
        ReflectionTestUtils.setField(invoice, "createdAt", createdAt);
        return invoice;
    }

    private Transaction transaction(TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);
        transaction.setTransactionDate(LocalDate.of(2026, 1, 10));
        return transaction;
    }

    private Stock stock(Integer productId) {
        Stock stock = new Stock();
        stock.setProductId(productId);
        return stock;
    }

    private Product product(Integer productId, String name) {
        Product product = new Product();
        product.setProductId(productId);
        product.setName(name);
        return product;
    }
}
