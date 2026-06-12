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
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceSeriesRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.NewProductRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.DiscountType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.InvoiceVatSummary;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Captor private ArgumentCaptor<List<InvoiceVatSummary>> vatSummaryCaptor;
    @Captor private ArgumentCaptor<List<InvoiceLineItem>> lineItemsCaptor;

    private static final Long COMPANY_ID = 1L;
    private static final Long CUSTOMER_ID = 7L;
    private static final Long INVOICE_ID = 100L;
    private static final Long ORIGINAL_ID = 10L;
    private static final Long RETURN_ID = 200L;

    private Company company;
    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.Ş.");

        activeCustomer = new Customer();
        activeCustomer.setCustomerId(CUSTOMER_ID);
        activeCustomer.setCompany(company);
        activeCustomer.setName("Aktif Müşteri");
        activeCustomer.setStatus(CustomerStatus.ACTIVE);
    }

    // ── createInvoice: toplamlar & KDV (applyTotals) ──────────────────────────

    @Test
    void createInvoice_singleLineTwentyPercentVatNoDiscount_computesExactTotals() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 2, "100.00", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        // 2 x 100.00 = 200.00; KDV %20 = 40.00; toplam 240.00
        assertThat(result.subtotal()).isEqualByComparingTo("200.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("40.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("240.00");
        assertThat(result.withholdingTaxAmount()).isEqualByComparingTo("0.00");
        // TRY faturada totalAmountTry = totalAmount
        assertThat(result.totalAmountTry()).isEqualByComparingTo("240.00");
        assertThat(result.lineItems()).hasSize(1);
        assertThat(result.lineItems().get(0).lineTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void createInvoice_mixedVatRates_createsPerRateVatSummaryRows() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", null));
        stubProduct(product(11, "10", "100.00", null));
        stubProduct(product(12, "1", "100.00", null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(
                lineReq(10, 1, "100.00", null, null),
                lineReq(11, 1, "100.00", null, null),
                lineReq(12, 1, "100.00", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.subtotal()).isEqualByComparingTo("300.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("31.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("331.00");

        InOrder order = inOrder(vatSummaryRepository);
        order.verify(vatSummaryRepository).deleteByInvoiceId(INVOICE_ID);
        order.verify(vatSummaryRepository).saveAll(vatSummaryCaptor.capture());
        List<InvoiceVatSummary> summaries = vatSummaryCaptor.getValue();
        assertThat(summaries).hasSize(3);
        assertThat(byRate(summaries, "20").getVatBaseAmount()).isEqualByComparingTo("100.00");
        assertThat(byRate(summaries, "20").getVatAmount()).isEqualByComparingTo("20.00");
        assertThat(byRate(summaries, "10").getVatBaseAmount()).isEqualByComparingTo("100.00");
        assertThat(byRate(summaries, "10").getVatAmount()).isEqualByComparingTo("10.00");
        assertThat(byRate(summaries, "1").getVatBaseAmount()).isEqualByComparingTo("100.00");
        assertThat(byRate(summaries, "1").getVatAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void createInvoice_lineDiscountTenPercent_vatComputedOnNetAfterDiscount() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "200.00", null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "200.00", "10", null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        // 200.00 - %10 = 180.00; KDV %20 net üzerinden = 36.00
        assertThat(result.subtotal()).isEqualByComparingTo("180.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("36.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("216.00");
    }

    @Test
    void createInvoice_percentageInvoiceDiscount_distributesProportionallyAcrossLines() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));
        stubProduct(product(11, "20", null, null));
        stubProduct(product(12, "20", null, null));

        // 33.33 + 33.33 + 33.34 = 100.00; %10 fatura iskontosu = 10.00 TL
        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(
                        lineReq(10, 1, "33.33", null, null),
                        lineReq(11, 1, "33.33", null, null),
                        lineReq(12, 1, "33.34", null, null)),
                DiscountType.PERCENTAGE, "10", null, null);
        InvoiceResponseDto result = service.createInvoice(dto);

        // Paylar: 3.33 / 3.33 / kalan 3.34 (son satır) -> netler 30.00 / 30.00 / 30.00
        verify(lineItemRepository).saveAll(lineItemsCaptor.capture());
        List<InvoiceLineItem> items = lineItemsCaptor.getValue();
        assertThat(items.get(0).getVatBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(items.get(1).getVatBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(items.get(2).getVatBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(result.subtotal()).isEqualByComparingTo("90.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("18.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("108.00");
    }

    @Test
    void createInvoice_invoiceDiscountRoundingRemainder_goesToLastLine() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));
        stubProduct(product(11, "20", null, null));
        stubProduct(product(12, "20", null, null));

        // 3 x 33.33 = 99.99; %10 iskonto = round2(9.9990) = 10.00
        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(
                        lineReq(10, 1, "33.33", null, null),
                        lineReq(11, 1, "33.33", null, null),
                        lineReq(12, 1, "33.33", null, null)),
                DiscountType.PERCENTAGE, "10", null, null);
        InvoiceResponseDto result = service.createInvoice(dto);

        // Paylar 3.33 / 3.33 / 3.34: yuvarlama artığı SON satıra gider -> son net 29.99
        verify(lineItemRepository).saveAll(lineItemsCaptor.capture());
        List<InvoiceLineItem> items = lineItemsCaptor.getValue();
        assertThat(items.get(0).getVatBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(items.get(1).getVatBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(items.get(2).getVatBaseAmount()).isEqualByComparingTo("29.99");
        assertThat(result.subtotal()).isEqualByComparingTo("89.99");
        assertThat(result.vatAmount()).isEqualByComparingTo("18.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("107.99");
    }

    @Test
    void createInvoice_fixedAmountDiscount_subtractedBeforeVat() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "500.00", null, null)),
                DiscountType.AMOUNT, "50.00", null, null);
        InvoiceResponseDto result = service.createInvoice(dto);

        // 500.00 - 50.00 = 450.00; KDV %20 = 90.00
        assertThat(result.subtotal()).isEqualByComparingTo("450.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("90.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("540.00");
    }

    @Test
    void createInvoice_discountExceedingSubtotal_isCappedAtSubtotal() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)),
                DiscountType.AMOUNT, "150.00", null, null);
        InvoiceResponseDto result = service.createInvoice(dto);

        // İskonto 150 > subtotal 100 -> 100'de kapanır; final 0, KDV 0
        assertThat(result.subtotal()).isEqualByComparingTo("0.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void createInvoice_withWithholding_reducesTotalAmount() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "1000.00", null, "20")));
        InvoiceResponseDto result = service.createInvoice(dto);

        // tevkifat = 1000 * %20 = 200; total = 1000 + 200 KDV - 200 tevkifat = 1000.00
        assertThat(result.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("200.00");
        assertThat(result.withholdingTaxAmount()).isEqualByComparingTo("200.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void createInvoice_withholdingComputedBeforeInvoiceLevelDiscountDistribution() {
        // Mevcut davranışı belgeler (InvoiceService applyTotals, ~684-687): tevkifat,
        // satır iskontosu SONRASI fakat fatura-geneli iskonto dağıtımından ÖNCEKİ baz
        // üzerinden hesaplanır. 1000 TL satır + 100 TL fatura iskontosu -> tevkifat
        // bazı 1000 (900 değil), yani %10 tevkifat = 100.00 (90.00 değil).
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "1000.00", null, "10")),
                DiscountType.AMOUNT, "100.00", null, null);
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.withholdingTaxAmount()).isEqualByComparingTo("100.00");
        assertThat(result.subtotal()).isEqualByComparingTo("900.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("180.00");
        // total = 900 + 180 - 100 = 980.00
        assertThat(result.totalAmount()).isEqualByComparingTo("980.00");
    }

    @Test
    void createInvoice_halfUpRoundingAtFiveThousandthsBoundary_roundsUp() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        // 3 x 33.335 = 100.005 -> HALF_UP scale 2 -> 100.01
        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 3, "33.335", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.subtotal()).isEqualByComparingTo("100.01");
        // KDV = round2(100.01 * 20 / 100 = 20.0020) = 20.00
        assertThat(result.vatAmount()).isEqualByComparingTo("20.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("120.01");
    }

    @Test
    void createInvoice_nullProductVatRate_fallsBackToZero() {
        stubCreateHappyPath();
        stubProduct(product(10, null, "100.00", null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("100.00");

        verify(vatSummaryRepository).saveAll(vatSummaryCaptor.capture());
        List<InvoiceVatSummary> summaries = vatSummaryCaptor.getValue();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getVatRate()).isEqualByComparingTo("0");
        assertThat(summaries.get(0).getVatBaseAmount()).isEqualByComparingTo("100.00");
    }

    // ── createInvoice: resolveUnitPrice zinciri ───────────────────────────────

    @Test
    void createInvoice_explicitUnitPrice_overridesProductPrices() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.purchase, List.of(lineReq(10, 1, "80.00", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.lineItems().get(0).unitPrice()).isEqualByComparingTo("80.00");
        assertThat(result.subtotal()).isEqualByComparingTo("80.00");
        // resolveUnitCost de açık fiyatı kullanır
        verify(stockMovementService).recordMovement(10, 1, MovementType.PURCHASE,
                "INVOICE", INVOICE_ID, new BigDecimal("80.00"), null);
    }

    @Test
    void createInvoice_purchaseWithoutUnitPrice_usesCostPrice() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.purchase, List.of(lineReq(10, 1, null, null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.lineItems().get(0).unitPrice()).isEqualByComparingTo("60.00");
        assertThat(result.subtotal()).isEqualByComparingTo("60.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("72.00");
    }

    @Test
    void createInvoice_saleWithoutUnitPrice_usesSalePriceNotCostPrice() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, null, null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.lineItems().get(0).unitPrice()).isEqualByComparingTo("100.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void createInvoice_missingSalePrice_fallsBackToCostPriceThenZero() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, "75.00")); // salePrice yok -> costPrice
        stubProduct(product(11, "20", null, null));    // hiçbiri yok -> ZERO

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(
                lineReq(10, 1, null, null, null),
                lineReq(11, 1, null, null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.lineItems().get(0).unitPrice()).isEqualByComparingTo("75.00");
        assertThat(result.lineItems().get(1).unitPrice()).isEqualByComparingTo("0");
        assertThat(result.subtotal()).isEqualByComparingTo("75.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("90.00");
    }

    // ── createInvoice: durum makinesi & validasyon ────────────────────────────

    @Test
    void createInvoice_whenInvoiceNumberAlreadyUsedInCompany_throwsAndSavesNothing() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId("INV-2026-0001", COMPANY_ID))
                .thenReturn(true);

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already used");

        verify(invoiceRepository, never()).save(any());
        verify(journalEntryService, never()).createForInvoice(any());
    }

    @Test
    void createInvoice_whenPeriodIsClosed_throwsClosedPeriodExceptionBeforeAnySave() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        doThrow(new ClosedPeriodException(2026, 6)).when(periodGuard).assertOpen(LocalDate.of(2026, 6, 1));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(ClosedPeriodException.class)
                .hasMessageContaining("2026");

        verify(invoiceRepository, never()).save(any());
        verify(stockMovementService, never()).recordMovement(any(), anyInt(), any(), any(), any(), any(), any());
        verify(journalEntryService, never()).createForInvoice(any());
    }

    @Test
    void createInvoice_whenCustomerBlocked_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        activeCustomer.setStatus(CustomerStatus.BLOCKED);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloke");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_whenCustomerPassive_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        activeCustomer.setStatus(CustomerStatus.PASSIVE);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pasif");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_saleInvoice_recordsNegativeStockMovementAndJournalOnce() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", "100.00", null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 3, "100.00", null, null)));
        service.createInvoice(dto);

        verify(stockMovementService).recordMovement(10, -3, MovementType.SALE,
                "INVOICE", INVOICE_ID, null, null);
        verify(journalEntryService, times(1)).createForInvoice(any(Invoice.class));
    }

    @Test
    void createInvoice_purchaseInvoice_recordsPositiveStockMovementWithUnitCost() {
        stubCreateHappyPath();
        Product p = product(10, "20", "100.00", "60.00");
        stubProduct(p);

        InvoiceRequestDto dto = invoiceReq(InvoiceType.purchase, List.of(lineReq(10, 4, null, null, null)));
        service.createInvoice(dto);

        verify(stockMovementService).recordMovement(10, 4, MovementType.PURCHASE,
                "INVOICE", INVOICE_ID, new BigDecimal("60.00"), null);
    }

    @Test
    void createInvoice_newProductOnSaleInvoice_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(newProductLine("BC-NEW", 1)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yeni ürün eklenemez");

        verify(invoiceRepository, never()).save(any());
        verify(productService, never()).createProductEntity(any());
    }

    @Test
    void createInvoice_duplicateBarcodeInSameRequest_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));
        when(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse("BC-DUP", COMPANY_ID))
                .thenReturn(false);
        Product created = product(50, "20", "100.00", "60.00");
        created.setBarcode("BC-DUP");
        when(productService.createProductEntity(any())).thenReturn(created);

        InvoiceRequestDto dto = invoiceReq(InvoiceType.purchase, List.of(
                newProductLine("BC-DUP", 1),
                newProductLine("BC-DUP", 2)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aynı request içinde");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoice_barcodeAlreadyInDatabase_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));
        when(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse("BC-EXISTS", COMPANY_ID))
                .thenReturn(true);

        InvoiceRequestDto dto = invoiceReq(InvoiceType.purchase, List.of(newProductLine("BC-EXISTS", 1)));

        assertThatThrownBy(() -> service.createInvoice(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten mevcut");

        verify(productService, never()).createProductEntity(any());
        verify(invoiceRepository, never()).save(any());
    }

    // ── confirmInvoice ────────────────────────────────────────────────────────

    @Test
    void confirmInvoice_whenNotDraft_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);

        assertThatThrownBy(() -> service.confirmInvoice(INVOICE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only draft invoices can be confirmed");

        verify(invoiceRepository, never()).save(any());
        verify(journalEntryService, never()).createForInvoice(any());
    }

    @Test
    void confirmInvoice_whenDraftHasNoLineItems_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.draft, InvoiceType.sale);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.confirmInvoice(INVOICE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no line items");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void confirmInvoice_saleDraft_recordsStockMovementsJournalAndSetsPending() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.draft, InvoiceType.sale);
        InvoiceLineItem li1 = lineItemEntity(10, 2, "100.00", "20", null, null);
        InvoiceLineItem li2 = lineItemEntity(11, 5, "50.00", "10", null, null);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(List.of(li1, li2));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.confirmInvoice(INVOICE_ID);

        verify(stockMovementService).recordMovement(10, -2, MovementType.SALE, "INVOICE", INVOICE_ID, null, null);
        verify(stockMovementService).recordMovement(11, -5, MovementType.SALE, "INVOICE", INVOICE_ID, null, null);
        verify(journalEntryService).createForInvoice(invoice);
        assertThat(result.paymentStatus()).isEqualTo("pending");
    }

    @Test
    void confirmInvoice_purchaseDraft_doesNotRecordStockMovement() {
        // Mevcut davranışı belgeler: confirmInvoice yalnızca satış faturalarında stok düşer;
        // alış taslağı onaylanırken stok hareketi YAZILMAZ.
        stubActiveInvoice(PaymentStatus.draft, InvoiceType.purchase);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(List.of(lineItemEntity(10, 2, "100.00", "20", null, null)));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.confirmInvoice(INVOICE_ID);

        verify(stockMovementService, never()).recordMovement(any(), anyInt(), any(), any(), any(), any(), any());
        verify(journalEntryService).createForInvoice(any(Invoice.class));
        assertThat(result.paymentStatus()).isEqualTo("pending");
    }

    // ── updateInvoice ─────────────────────────────────────────────────────────

    @Test
    void updateInvoice_whenPaid_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.paid, InvoiceType.sale);

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));

        assertThatThrownBy(() -> service.updateInvoice(INVOICE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paid invoices cannot be updated");

        verify(invoiceRepository, never()).save(any());
        verify(journalEntryService, never()).reverseForInvoice(any(), any(), any());
    }

    @Test
    void updateInvoice_whenNewNumberConflicts_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId("INV-9999", COMPANY_ID))
                .thenReturn(true);

        InvoiceRequestDto dto = new InvoiceRequestDto("INV-9999", CUSTOMER_ID, InvoiceType.sale,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1),
                List.of(lineReq(10, 1, "100.00", null, null)), null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateInvoice(INVOICE_ID, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already used");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updateInvoice_whenValid_reversesThenRecreatesJournalInOrder() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));
        InvoiceResponseDto result = service.updateInvoice(INVOICE_ID, dto);

        InOrder order = inOrder(journalEntryService);
        order.verify(journalEntryService).reverseForInvoice(COMPANY_ID, INVOICE_ID, "Güncelleme");
        order.verify(journalEntryService).createForInvoice(invoice);
        assertThat(result.invoiceNumber()).isEqualTo("INV-2026-0001");
    }

    // ── deleteInvoice ─────────────────────────────────────────────────────────

    @Test
    void deleteInvoice_whenPaid_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.paid, InvoiceType.sale);

        assertThatThrownBy(() -> service.deleteInvoice(INVOICE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paid invoices cannot be deleted");

        verify(invoiceRepository, never()).save(any());
        verify(stockMovementService, never()).recordReverseMovementsForInvoice(any());
    }

    @Test
    void deleteInvoice_whenPartiallyPaid_throwsBusinessException() {
        stubActiveInvoice(PaymentStatus.partially_paid, InvoiceType.sale);

        assertThatThrownBy(() -> service.deleteInvoice(INVOICE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ödeme bulunan");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void deleteInvoice_whenValid_softDeletesLinesReversesStockAndJournal() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        InvoiceLineItem li1 = lineItemEntity(10, 2, "100.00", "20", null, null);
        InvoiceLineItem li2 = lineItemEntity(11, 1, "50.00", "10", null, null);
        List<InvoiceLineItem> lines = List.of(li1, li2);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(lines);

        service.deleteInvoice(INVOICE_ID);

        assertThat(li1.isDeleted()).isTrue();
        assertThat(li2.isDeleted()).isTrue();
        verify(lineItemRepository).saveAll(lines);
        verify(stockMovementService).recordReverseMovementsForInvoice(INVOICE_ID);
        assertThat(invoice.isDeleted()).isTrue();
        assertThat(invoice.getDeletedAt()).isNotNull();
        verify(invoiceRepository).save(invoice);
        verify(journalEntryService).reverseForInvoice(COMPANY_ID, INVOICE_ID, "Fatura silindi");
    }

    // ── cancelInvoice ─────────────────────────────────────────────────────────

    @Test
    void cancelInvoice_whenAlreadyCancelled_throwsBusinessException() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        invoice.setCancelled(true);

        assertThatThrownBy(() -> service.cancelInvoice(INVOICE_ID, "Sebep"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten iptal");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_whenHasPayments_throwsBusinessException() {
        stubActiveInvoice(PaymentStatus.paid, InvoiceType.sale);

        assertThatThrownBy(() -> service.cancelInvoice(INVOICE_ID, "Sebep"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("iptal edilemez");

        verify(stockMovementService, never()).recordReverseMovementsForInvoice(any());
    }

    @Test
    void cancelInvoice_whenValid_setsCancellationFieldsAndReversesEverything() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        InvoiceLineItem li = lineItemEntity(10, 2, "100.00", "20", null, null);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(INVOICE_ID, COMPANY_ID))
                .thenReturn(List.of(li));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.cancelInvoice(INVOICE_ID, "Müşteri talebi");

        assertThat(invoice.isCancelled()).isTrue();
        assertThat(invoice.getCancellationReason()).isEqualTo("Müşteri talebi");
        assertThat(invoice.getCancelledAt()).isNotNull();
        assertThat(li.isDeleted()).isTrue();
        verify(stockMovementService).recordReverseMovementsForInvoice(INVOICE_ID);
        verify(journalEntryService).reverseForInvoice(COMPANY_ID, INVOICE_ID, "İptal: Müşteri talebi");
        assertThat(result.cancelled()).isTrue();
    }

    // ── updatePaymentStatus ───────────────────────────────────────────────────

    @Test
    void updatePaymentStatus_toDraft_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);

        assertThatThrownBy(() -> service.updatePaymentStatus(INVOICE_ID, PaymentStatus.draft))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot set payment status to draft");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_toPaid_throwsUsePaymentsEndpoint() {
        stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);

        assertThatThrownBy(() -> service.updatePaymentStatus(INVOICE_ID, PaymentStatus.paid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("/payments");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_draftToPending_throwsUseConfirmEndpoint() {
        stubActiveInvoice(PaymentStatus.draft, InvoiceType.sale);

        assertThatThrownBy(() -> service.updatePaymentStatus(INVOICE_ID, PaymentStatus.pending))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("/confirm");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_pendingToOverdue_updatesStatus() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.updatePaymentStatus(INVOICE_ID, PaymentStatus.overdue);

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.overdue);
        assertThat(result.paymentStatus()).isEqualTo("overdue");
    }

    // ── restoreInvoice ────────────────────────────────────────────────────────

    @Test
    void restoreInvoice_whenNotDeleted_throwsRuntimeException() {
        stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);

        assertThatThrownBy(() -> service.restoreInvoice(INVOICE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not deleted");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void restoreInvoice_whenDeleted_clearsDeletionFields() {
        Invoice invoice = stubActiveInvoice(PaymentStatus.pending, InvoiceType.sale);
        invoice.setDeleted(true);
        invoice.setDeletedAt(LocalDateTime.now());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto result = service.restoreInvoice(INVOICE_ID);

        assertThat(invoice.isDeleted()).isFalse();
        assertThat(invoice.getDeletedAt()).isNull();
        assertThat(result.isDeleted()).isFalse();
        verify(invoiceRepository).save(invoice);
    }

    // ── tenancy ───────────────────────────────────────────────────────────────

    @Test
    void getInvoiceById_whenInvoiceBelongsToAnotherCompany_throwsAccessDenied() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        Company otherCompany = new Company();
        otherCompany.setCompanyId(2L);
        Invoice foreign = invoiceEntity(INVOICE_ID, PaymentStatus.pending, InvoiceType.sale);
        foreign.setCompany(otherCompany);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.getInvoiceById(INVOICE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("görüntüleme yetkiniz yok");
    }

    // ── createReturnInvoice ───────────────────────────────────────────────────

    @Test
    void createReturnInvoice_whenOriginalCancelled_throwsBusinessException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        Invoice original = invoiceEntity(ORIGINAL_ID, PaymentStatus.pending, InvoiceType.sale);
        original.setCancelled(true);
        when(invoiceRepository.findById(ORIGINAL_ID)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.createReturnInvoice(ORIGINAL_ID, returnReq("RET-2026-0001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("İptal edilmiş faturaya iade yapılamaz");

        verify(invoiceRepository, never()).save(any());
        verify(journalEntryService, never()).createForInvoice(any());
    }

    @Test
    void createReturnInvoice_saleOriginal_createsPurchaseReturnWithPositiveStockMovement() {
        Invoice original = stubReturnHappyPath(InvoiceType.sale,
                List.of(lineItemEntity(10, 3, "100.00", "20", null, null)));
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceResponseDto result = service.createReturnInvoice(ORIGINAL_ID, returnReq("RET-2026-0001"));

        assertThat(result.invoiceType()).isEqualTo("purchase");
        assertThat(result.referenceInvoiceId()).isEqualTo(ORIGINAL_ID);
        assertThat(result.paymentStatus()).isEqualTo("pending");
        assertThat(result.description()).contains(original.getInvoiceNumber());
        // Satış iadesi = mal geri gelir -> pozitif PURCHASE hareketi, maliyet = costPrice
        verify(stockMovementService).recordMovement(10, 3, MovementType.PURCHASE,
                "INVOICE", RETURN_ID, new BigDecimal("60.00"), null);
        verify(journalEntryService).createForInvoice(any(Invoice.class));
    }

    @Test
    void createReturnInvoice_purchaseOriginal_createsSaleReturnWithNegativeStockMovement() {
        stubReturnHappyPath(InvoiceType.purchase,
                List.of(lineItemEntity(10, 3, "100.00", "20", null, null)));
        stubProduct(product(10, "20", "100.00", "60.00"));

        InvoiceResponseDto result = service.createReturnInvoice(ORIGINAL_ID, returnReq("RET-2026-0002"));

        assertThat(result.invoiceType()).isEqualTo("sale");
        // Alış iadesi = mal geri gider -> negatif SALE hareketi
        verify(stockMovementService).recordMovement(10, -3, MovementType.SALE,
                "INVOICE", RETURN_ID, new BigDecimal("60.00"), null);
    }

    @Test
    void createReturnInvoice_copiesLineRatesCurrencyAndReference() {
        Invoice original = stubReturnHappyPath(InvoiceType.sale,
                List.of(lineItemEntity(10, 2, "200.00", "10", "10", "5")));
        original.setCurrency(Currency.USD);
        original.setExchangeRate(new BigDecimal("30"));
        stubProduct(product(10, "10", "200.00", "120.00"));

        InvoiceResponseDto result = service.createReturnInvoice(ORIGINAL_ID, returnReq("RET-2026-0003"));

        verify(lineItemRepository).saveAll(lineItemsCaptor.capture());
        InvoiceLineItem copied = lineItemsCaptor.getValue().get(0);
        assertThat(copied.getQuantity()).isEqualTo(2);
        assertThat(copied.getUnitPrice()).isEqualByComparingTo("200.00");
        assertThat(copied.getVatRate()).isEqualByComparingTo("10");
        assertThat(copied.getDiscountRate()).isEqualByComparingTo("10");
        assertThat(copied.getWithholdingTaxRate()).isEqualByComparingTo("5");

        // 2x200=400; satır iskontosu %10 -> 360.00; KDV %10 = 36.00; tevkifat %5 = 18.00
        assertThat(result.subtotal()).isEqualByComparingTo("360.00");
        assertThat(result.vatAmount()).isEqualByComparingTo("36.00");
        assertThat(result.withholdingTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("378.00");
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.exchangeRate()).isEqualByComparingTo("30");
        assertThat(result.totalAmountTry()).isEqualByComparingTo("11340.00");
        assertThat(result.referenceInvoiceId()).isEqualTo(ORIGINAL_ID);
    }

    @Test
    void createReturnInvoice_lineWithDeletedProduct_isSilentlySkipped() {
        // Mevcut davranışı belgeler: silinmiş ürünlü orijinal satır iade faturasına
        // kopyalanmaz ve hata da fırlatılmaz (sessizce atlanır).
        stubReturnHappyPath(InvoiceType.sale, List.of(
                lineItemEntity(10, 1, "100.00", "20", null, null),
                lineItemEntity(11, 1, "50.00", "20", null, null)));
        stubProduct(product(10, "20", "100.00", "60.00"));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(11, COMPANY_ID))
                .thenReturn(Optional.empty());

        InvoiceResponseDto result = service.createReturnInvoice(ORIGINAL_ID, returnReq("RET-2026-0004"));

        verify(lineItemRepository).saveAll(lineItemsCaptor.capture());
        assertThat(lineItemsCaptor.getValue()).hasSize(1);
        assertThat(lineItemsCaptor.getValue().get(0).getProductId()).isEqualTo(10);
        verify(stockMovementService, times(1)).recordMovement(any(), anyInt(), any(), any(), any(), any(), any());
        assertThat(result.lineItems()).hasSize(1);
        assertThat(result.subtotal()).isEqualByComparingTo("100.00");
    }

    // ── çoklu para birimi ─────────────────────────────────────────────────────

    @Test
    void createInvoice_usdWithExchangeRate_totalAmountTryMultipliedByRate() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)),
                null, null, Currency.USD, "30");
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.exchangeRate()).isEqualByComparingTo("30");
        assertThat(result.totalAmount()).isEqualByComparingTo("120.00");
        assertThat(result.totalAmountTry()).isEqualByComparingTo("3600.00");
    }

    @Test
    void createInvoice_nullCurrency_defaultsToTryWithRateOne() {
        stubCreateHappyPath();
        stubProduct(product(10, "20", null, null));

        InvoiceRequestDto dto = invoiceReq(InvoiceType.sale, List.of(lineReq(10, 1, "100.00", null, null)));
        InvoiceResponseDto result = service.createInvoice(dto);

        assertThat(result.currency()).isEqualTo("TRY");
        assertThat(result.exchangeRate()).isEqualByComparingTo("1");
        assertThat(result.totalAmountTry()).isEqualByComparingTo(result.totalAmount());
    }

    // ── seri yönetimi ─────────────────────────────────────────────────────────

    @Test
    void assignNextInvoiceNumber_incrementsSequenceAndFormatsWithPrefix() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoiceSeries series = series("FTR", "FTR", 41L);
        when(seriesRepository.findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "FTR", InvoiceType.sale))
                .thenReturn(Optional.of(series));

        String number = service.assignNextInvoiceNumber(InvoiceType.sale, "FTR");

        assertThat(number).isEqualTo("FTR-000042");
        assertThat(series.getLastSequenceNumber()).isEqualTo(42L);
        verify(seriesRepository).save(series);
    }

    @Test
    void assignNextInvoiceNumber_nullPrefix_fallsBackToSeriesCode() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        InvoiceSeries series = series("A", null, 0L);
        when(seriesRepository.findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "A", InvoiceType.sale))
                .thenReturn(Optional.of(series));

        String number = service.assignNextInvoiceNumber(InvoiceType.sale, "A");

        assertThat(number).isEqualTo("A-000001");
    }

    @Test
    void assignNextInvoiceNumber_whenSeriesMissing_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(seriesRepository.findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "YOK", InvoiceType.sale))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignNextInvoiceNumber(InvoiceType.sale, "YOK"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Active series not found");

        verify(seriesRepository, never()).save(any());
    }

    @Test
    void createSeries_whenCodeAndTypeAlreadyExist_throwsRuntimeException() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(seriesRepository.existsByCompanyCompanyIdAndSeriesCodeAndInvoiceType(COMPANY_ID, "FTR", InvoiceType.sale))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSeries(
                new InvoiceSeriesRequestDto("FTR", InvoiceType.sale, "FTR", 2026, true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(seriesRepository, never()).save(any());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private void stubCreateHappyPath() {
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId(anyString(), eq(COMPANY_ID)))
                .thenReturn(false);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(CUSTOMER_ID, COMPANY_ID))
                .thenReturn(Optional.of(activeCustomer));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getInvoiceId() == null) i.setInvoiceId(INVOICE_ID);
            return i;
        });
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Invoice stubReturnHappyPath(InvoiceType originalType, List<InvoiceLineItem> originalLines) {
        Invoice original = invoiceEntity(ORIGINAL_ID, PaymentStatus.pending, originalType);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(ORIGINAL_ID)).thenReturn(Optional.of(original));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(ORIGINAL_ID, COMPANY_ID))
                .thenReturn(originalLines);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getInvoiceId() == null) i.setInvoiceId(RETURN_ID);
            return i;
        });
        when(lineItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        return original;
    }

    private Invoice stubActiveInvoice(PaymentStatus status, InvoiceType type) {
        Invoice invoice = invoiceEntity(INVOICE_ID, status, type);
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private Invoice invoiceEntity(Long id, PaymentStatus status, InvoiceType type) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setInvoiceNumber("INV-2026-0001");
        invoice.setCustomerId(CUSTOMER_ID);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 1));
        invoice.setDueDate(LocalDate.of(2026, 7, 1));
        invoice.setPaymentStatus(status);
        invoice.setCancelled(false);
        invoice.setCurrency(Currency.TRY);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setVatAmount(new BigDecimal("20.00"));
        invoice.setTotalAmount(new BigDecimal("120.00"));
        return invoice;
    }

    private InvoiceLineItem lineItemEntity(int productId, int quantity, String unitPrice,
                                           String vatRate, String discountRate, String wtRate) {
        InvoiceLineItem li = new InvoiceLineItem();
        li.setCompany(company);
        li.setInvoiceId(INVOICE_ID);
        li.setProductId(productId);
        li.setQuantity(quantity);
        li.setUnitPrice(new BigDecimal(unitPrice));
        li.setVatRate(vatRate != null ? new BigDecimal(vatRate) : null);
        li.setLineTotal(BigDecimal.ZERO);
        li.setDiscountRate(discountRate != null ? new BigDecimal(discountRate) : null);
        li.setWithholdingTaxRate(wtRate != null ? new BigDecimal(wtRate) : null);
        li.setDeleted(false);
        return li;
    }

    private Product product(int id, String vatRate, String salePrice, String costPrice) {
        return Product.builder()
                .productId(id)
                .company(company)
                .barcode("BC-" + id)
                .name("Ürün " + id)
                .vatRate(vatRate != null ? new BigDecimal(vatRate) : null)
                .salePrice(salePrice != null ? new BigDecimal(salePrice) : null)
                .costPrice(costPrice != null ? new BigDecimal(costPrice) : null)
                .build();
    }

    private void stubProduct(Product product) {
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(product.getProductId(), COMPANY_ID))
                .thenReturn(Optional.of(product));
    }

    private InvoiceSeries series(String code, String prefix, long lastSequence) {
        return InvoiceSeries.builder()
                .seriesId(1L)
                .company(company)
                .seriesCode(code)
                .invoiceType(InvoiceType.sale)
                .prefix(prefix)
                .year(2026)
                .lastSequenceNumber(lastSequence)
                .isActive(true)
                .build();
    }

    private InvoiceLineItemRequestDto lineReq(Integer productId, int quantity, String unitPrice,
                                              String discountRate, String wtRate) {
        return new InvoiceLineItemRequestDto(productId, quantity, null,
                unitPrice != null ? new BigDecimal(unitPrice) : null,
                discountRate != null ? new BigDecimal(discountRate) : null,
                wtRate != null ? new BigDecimal(wtRate) : null,
                null, null, null);
    }

    private InvoiceLineItemRequestDto newProductLine(String barcode, int quantity) {
        NewProductRequestDto newProduct = new NewProductRequestDto(barcode, "Yeni Ürün", null, "Adet",
                new BigDecimal("100.00"), new BigDecimal("20"), new BigDecimal("60.00"), 0);
        return new InvoiceLineItemRequestDto(null, quantity, newProduct, null, null, null, null, null, null);
    }

    private InvoiceRequestDto invoiceReq(InvoiceType type, List<InvoiceLineItemRequestDto> lines) {
        return invoiceReq(type, lines, null, null, null, null);
    }

    private InvoiceRequestDto invoiceReq(InvoiceType type, List<InvoiceLineItemRequestDto> lines,
                                         DiscountType discountType, String discountAmount,
                                         Currency currency, String exchangeRate) {
        return new InvoiceRequestDto("INV-2026-0001", CUSTOMER_ID, type,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), lines,
                currency, exchangeRate != null ? new BigDecimal(exchangeRate) : null,
                discountType, discountAmount != null ? new BigDecimal(discountAmount) : null,
                null, null);
    }

    private InvoiceRequestDto returnReq(String invoiceNumber) {
        return new InvoiceRequestDto(invoiceNumber, CUSTOMER_ID, InvoiceType.sale,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), List.of(),
                null, null, null, null, null, "İade adresi");
    }

    private InvoiceVatSummary byRate(List<InvoiceVatSummary> summaries, String rate) {
        return summaries.stream()
                .filter(s -> s.getVatRate().compareTo(new BigDecimal(rate)) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("KDV oranı için özet satırı yok: " + rate));
    }
}
