package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.exception.ClosedPeriodException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.dto.request.CollectionPromiseRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceSeriesRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.NewProductRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.CollectionPromiseResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.CollectionPromise;
import com.MuhasebePlus.demo.invoice.entity.DiscountType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.CollectionPromiseRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceSeriesRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceVatSummaryRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.service.ProductService;
import com.MuhasebePlus.demo.stock.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository lineItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementService stockMovementService;
    @Mock private ProductService productService;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private SystemLogService systemLogService;
    @Mock private InvoiceSeriesRepository seriesRepository;
    @Mock private CollectionPromiseRepository promiseRepository;
    @Mock private AccountingPeriodGuard periodGuard;
    @Mock private JournalEntryService journalEntryService;
    @Mock private InvoiceVatSummaryRepository vatSummaryRepository;

    @InjectMocks
    private InvoiceService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;
    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        customer = new Customer();
        customer.setCustomerId(10L);
        customer.setCompany(company);
        customer.setName("ABC Ltd");
        customer.setStatus(CustomerStatus.ACTIVE);

        product = Product.builder()
                .productId(5)
                .company(company)
                .barcode("PRD-5")
                .name("Kalem")
                .unit("adet")
                .salePrice(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("60.00"))
                .vatRate(new BigDecimal("20.00"))
                .build();

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
    }

    // Tests sale invoice creation calculates discount, VAT, stock movement, and journal entry.
    @Test
    void createInvoice_whenSaleInvoiceIsValid_calculatesTotalsAndRecordsStockMovement() {
        InvoiceRequestDto dto = invoiceRequest(
                "FTR-001",
                InvoiceType.sale,
                List.of(new InvoiceLineItemRequestDto(5, 2, null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null)),
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00")
        );
        stubCustomerAndProduct();
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId("FTR-001", COMPANY_ID)).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(inv -> saveInvoice(inv.getArgument(0), 100L));
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.invoiceId()).isEqualTo(100L);
        assertThat(result.subtotal()).isEqualByComparingTo("180.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("36.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("216.00");
        verify(stockMovementService).recordMovement(5, -2, MovementType.SALE, "INVOICE", 100L, null, null);
        verify(journalEntryService).createForInvoice(any(Invoice.class));
    }

    // Tests closed invoice period stops creation before persistence.
    @Test
    void createInvoice_whenPeriodIsClosed_throwsBeforeSaving() {
        InvoiceRequestDto dto = invoiceRequest("FTR-CLOSED", InvoiceType.sale,
                List.of(new InvoiceLineItemRequestDto(5, 1, null, null, null, null, null, null, null)),
                null, null);
        org.mockito.Mockito.doThrow(new ClosedPeriodException(2026, 5))
                .when(periodGuard).assertOpen(dto.invoiceDate());

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(ClosedPeriodException.class);

        verify(invoiceRepository, never()).save(any());
    }

    // Tests blocked customers cannot receive invoices.
    @Test
    void createInvoice_whenCustomerIsBlocked_throwsBusinessException() {
        customer.setStatus(CustomerStatus.BLOCKED);
        InvoiceRequestDto dto = invoiceRequest("FTR-BLOCKED", InvoiceType.sale,
                List.of(new InvoiceLineItemRequestDto(5, 1, null, null, null, null, null, null, null)),
                null, null);
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId("FTR-BLOCKED", COMPANY_ID)).thenReturn(false);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloke");

        verify(invoiceRepository, never()).save(any());
    }

    // Tests purchase invoice can create a new product and records purchase stock movement with unit cost.
    @Test
    void createInvoice_whenPurchaseHasNewProduct_createsProductAndRecordsPurchaseMovement() {
        NewProductRequestDto newProduct = new NewProductRequestDto(
                "NEW-1", "Yeni Urun", "Aciklama", "adet",
                new BigDecimal("150.00"), new BigDecimal("10.00"), new BigDecimal("80.00"), 3);
        Product createdProduct = Product.builder()
                .productId(8)
                .company(company)
                .barcode("NEW-1")
                .name("Yeni Urun")
                .salePrice(new BigDecimal("150.00"))
                .costPrice(new BigDecimal("80.00"))
                .vatRate(new BigDecimal("10.00"))
                .build();
        InvoiceRequestDto dto = invoiceRequest("ALI-001", InvoiceType.purchase,
                List.of(new InvoiceLineItemRequestDto(null, 4, newProduct, null, null, null, null, null, null)),
                null, null);
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId("ALI-001", COMPANY_ID)).thenReturn(false);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));
        when(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse("NEW-1", COMPANY_ID))
                .thenReturn(false);
        when(productService.createProductEntity(any())).thenReturn(createdProduct);
        when(invoiceRepository.save(any())).thenAnswer(inv -> saveInvoice(inv.getArgument(0), 101L));
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.subtotal()).isEqualByComparingTo("320.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("32.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("352.00");
        verify(stockMovementService).recordMovement(8, 4, MovementType.PURCHASE, "INVOICE", 101L, new BigDecimal("80.00"), null);
    }

    // Tests only draft invoices can be confirmed.
    @Test
    void confirmInvoice_whenInvoiceIsNotDraft_throws() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.confirmInvoice(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("draft");
    }

    // Tests confirming draft sale invoice records stock movement and creates journal entry.
    @Test
    void confirmInvoice_whenDraftSaleHasLines_movesStockAndCreatesJournal() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.draft);
        InvoiceLineItem line = line(100L, 5, 3, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, COMPANY_ID))
                .thenReturn(List.of(line));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findByProductIdInAndCompanyCompanyId(List.of(5), COMPANY_ID))
                .thenReturn(List.of(product));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));

        InvoiceResponseDto result = service.confirmInvoice(100L);

        assertThat(result.paymentStatus()).isEqualTo("pending");
        verify(stockMovementService).recordMovement(5, -3, MovementType.SALE, "INVOICE", 100L, null, null);
        verify(journalEntryService).createForInvoice(invoice);
    }

    // Tests paid invoices cannot be updated.
    @Test
    void updateInvoice_whenInvoiceIsPaid_throwsBeforeSaving() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.paid);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.updateInvoice(100L, invoiceRequest("FTR-NEW", InvoiceType.sale,
                List.of(new InvoiceLineItemRequestDto(5, 1, null, null, null, null, null, null, null)), null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paid invoices");

        verify(invoiceRepository, never()).save(any());
    }

    // Tests partially paid invoices cannot be deleted because payment integrity must be preserved.
    @Test
    void deleteInvoice_whenPartiallyPaid_throwsBusinessException() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.partially_paid);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.deleteInvoice(100L))
                .isInstanceOf(BusinessException.class);

        verify(invoiceRepository, never()).save(any());
    }

    // Tests deleting an unpaid invoice soft-deletes line items, reverses stock, and reverses journal.
    @Test
    void deleteInvoice_whenPending_softDeletesLinesAndReversesRelatedEntries() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        InvoiceLineItem line = line(100L, 5, 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, COMPANY_ID))
                .thenReturn(List.of(line));
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteInvoice(100L);

        assertThat(invoice.isDeleted()).isTrue();
        assertThat(line.isDeleted()).isTrue();
        verify(stockMovementService).recordReverseMovementsForInvoice(100L);
        verify(journalEntryService).reverseForInvoice(eq(COMPANY_ID), eq(100L), any());
    }

    // Tests invoice cancellation marks cancellation fields and reverses stock and journal.
    @Test
    void cancelInvoice_whenPending_marksCancelledAndDeletesLines() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        InvoiceLineItem line = line(100L, 5, 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, COMPANY_ID))
                .thenReturn(List.of(line));
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findByProductIdInAndCompanyCompanyId(List.of(5), COMPANY_ID))
                .thenReturn(List.of(product));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));

        InvoiceResponseDto result = service.cancelInvoice(100L, "Musteri iptali");

        assertThat(result.cancelled()).isTrue();
        assertThat(result.cancellationReason()).isEqualTo("Musteri iptali");
        assertThat(line.isDeleted()).isTrue();
        verify(stockMovementService).recordReverseMovementsForInvoice(100L);
        verify(journalEntryService).reverseForInvoice(eq(COMPANY_ID), eq(100L), any());
    }

    // Tests return invoice from a sale becomes purchase return and restores stock.
    @Test
    void createReturnInvoice_whenOriginalSale_createsPurchaseReturnAndMovesStockIn() {
        Invoice original = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        original.setInvoiceNumber("FTR-001");
        InvoiceLineItem originalLine = line(100L, 5, 2, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(original));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, COMPANY_ID))
                .thenReturn(List.of(originalLine));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(invoiceRepository.save(any())).thenAnswer(inv -> saveInvoice(inv.getArgument(0), 200L));
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));

        InvoiceResponseDto result = service.createReturnInvoice(100L, invoiceRequest("IADE-001", InvoiceType.purchase,
                List.of(new InvoiceLineItemRequestDto(5, 2, null, null, null, null, null, null, null)), null, null));

        assertThat(result.invoiceType()).isEqualTo("purchase");
        assertThat(result.referenceInvoiceId()).isEqualTo(100L);
        assertThat(result.totalAmount()).isEqualByComparingTo("240.00");
        verify(stockMovementService).recordMovement(5, 2, MovementType.PURCHASE, "INVOICE", 200L, new BigDecimal("60.00"), null);
    }

    // Tests paged invoice query uses search branch and maps line items with products.
    @Test
    void getInvoicesPaged_whenSearchProvided_usesSearchRepositoryAndMapsLines() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        InvoiceLineItem line = line(100L, 5, 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(invoiceRepository.searchInvoices(eq(COMPANY_ID), eq("FTR"), any()))
                .thenReturn(new PageImpl<>(List.of(invoice), PageRequest.of(0, 10), 1));
        when(lineItemRepository.findByInvoiceIdInAndCompanyAndActive(List.of(100L), COMPANY_ID))
                .thenReturn(List.of(line));
        when(productRepository.findByProductIdInAndCompanyCompanyId(List.of(5), COMPANY_ID))
                .thenReturn(List.of(product));

        Page<InvoiceResponseDto> result = service.getInvoicesPaged(
                null, null, null, null, "FTR", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).lineItems()).hasSize(1);
        assertThat(result.getContent().get(0).lineItems().get(0).productName()).isEqualTo("Kalem");
    }

    // Tests invoice series creation rejects duplicate series codes per type.
    @Test
    void createSeries_whenDuplicateSeriesExists_throws() {
        InvoiceSeriesRequestDto dto = new InvoiceSeriesRequestDto("A", InvoiceType.sale, "SAT", 2026, true);
        when(seriesRepository.existsByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "A", InvoiceType.sale))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSeries(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("series");

        verify(seriesRepository, never()).save(any());
    }

    // Tests next invoice number increments and formats the active series.
    @Test
    void assignNextInvoiceNumber_whenSeriesExists_incrementsAndFormatsNumber() {
        InvoiceSeries series = InvoiceSeries.builder()
                .company(company)
                .seriesCode("A")
                .invoiceType(InvoiceType.sale)
                .prefix("SAT")
                .year(2026)
                .lastSequenceNumber(41L)
                .isActive(true)
                .build();
        when(seriesRepository.findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "A", InvoiceType.sale))
                .thenReturn(Optional.of(series));

        String nextNumber = service.assignNextInvoiceNumber(InvoiceType.sale, "A");

        assertThat(nextNumber).isEqualTo("SAT-000042");
        verify(seriesRepository).save(series);
    }

    // Tests collection promise creation validates invoice access and persists the promise.
    @Test
    void createCollectionPromise_whenInvoiceExists_savesPromise() {
        Invoice invoice = invoice(100L, InvoiceType.sale, PaymentStatus.pending);
        CollectionPromiseRequestDto dto = new CollectionPromiseRequestDto(
                LocalDate.now().plusDays(7), new BigDecimal("500.00"), "Haftaya odeme");
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(promiseRepository.save(any())).thenAnswer(inv -> {
            CollectionPromise promise = inv.getArgument(0);
            promise.setPromiseId(300L);
            return promise;
        });

        CollectionPromiseResponseDto result = service.createCollectionPromise(100L, dto);

        assertThat(result.promiseId()).isEqualTo(300L);
        assertThat(result.promisedAmount()).isEqualByComparingTo("500.00");
        assertThat(result.fulfilled()).isFalse();
    }

    // Tests fulfilling a promise stamps fulfilled status and date.
    @Test
    void fulfillCollectionPromise_whenPromiseExists_marksFulfilled() {
        CollectionPromise promise = CollectionPromise.builder()
                .promiseId(300L)
                .company(company)
                .invoiceId(100L)
                .promisedDate(LocalDate.now())
                .promisedAmount(new BigDecimal("500.00"))
                .fulfilled(false)
                .build();
        when(promiseRepository.findById(300L)).thenReturn(Optional.of(promise));

        service.fulfillCollectionPromise(300L);

        assertThat(promise.isFulfilled()).isTrue();
        assertThat(promise.getFulfilledAt()).isEqualTo(LocalDate.now());
        verify(promiseRepository).save(promise);
    }

    // Tests hard delete removes line items before deleting expired invoices.
    @Test
    void hardDeleteExpired_whenExpiredInvoicesExist_deletesLinesAndInvoices() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        Invoice first = invoice(1L, InvoiceType.sale, PaymentStatus.pending);
        Invoice second = invoice(2L, InvoiceType.purchase, PaymentStatus.pending);
        when(invoiceRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(lineItemRepository).deleteByInvoiceId(1L);
        verify(lineItemRepository).deleteByInvoiceId(2L);
        verify(invoiceRepository).delete(first);
        verify(invoiceRepository).delete(second);
    }

    private void stubCustomerAndProduct() {
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(customer));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
    }

    private InvoiceRequestDto invoiceRequest(String number, InvoiceType type, List<InvoiceLineItemRequestDto> lines,
                                             DiscountType discountType, BigDecimal discountAmount) {
        return new InvoiceRequestDto(
                number,
                10L,
                type,
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 6, 20),
                lines,
                Currency.TRY,
                BigDecimal.ONE,
                discountType,
                discountAmount,
                "Aciklama",
                "Teslimat adresi"
        );
    }

    private Invoice saveInvoice(Invoice invoice, Long id) {
        if (invoice.getInvoiceId() == null) {
            invoice.setInvoiceId(id);
        }
        return invoice;
    }

    private Invoice invoice(Long id, InvoiceType type, PaymentStatus status) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("FTR-" + id);
        invoice.setCustomerId(10L);
        invoice.setCustomer(customer);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 5, 20));
        invoice.setDueDate(LocalDate.of(2026, 6, 20));
        invoice.setPaymentStatus(status);
        invoice.setSubtotal(new BigDecimal("200.00"));
        invoice.setVatAmount(new BigDecimal("40.00"));
        invoice.setTotalAmount(new BigDecimal("240.00"));
        invoice.setCurrency(Currency.TRY);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setWithholdingTaxAmount(BigDecimal.ZERO);
        invoice.setCancelled(false);
        invoice.setDeleted(false);
        return invoice;
    }

    private InvoiceLineItem line(Long invoiceId, Integer productId, int quantity,
                                 BigDecimal unitPrice, BigDecimal discountRate, BigDecimal withholdingRate) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setCompany(company);
        line.setInvoiceId(invoiceId);
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setVatRate(new BigDecimal("20.00"));
        line.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(new BigDecimal("1.20")));
        line.setDiscountRate(discountRate);
        line.setWithholdingTaxRate(withholdingRate);
        line.setDeleted(false);
        return line;
    }
}
