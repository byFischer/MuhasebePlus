package com.MuhasebePlus.demo.vat.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.period.service.AccountingPeriodGuard;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import com.MuhasebePlus.demo.vat.dto.BaBsEntryDto;
import com.MuhasebePlus.demo.vat.dto.VatCalculateRequestDto;
import com.MuhasebePlus.demo.vat.dto.VatPeriodResponseDto;
import com.MuhasebePlus.demo.vat.entity.VatDeclarationLine;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import com.MuhasebePlus.demo.vat.repository.VatDeclarationLineRepository;
import com.MuhasebePlus.demo.vat.repository.VatPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VatDeclarationServiceTest {

    @Mock private VatPeriodRepository vatPeriodRepository;
    @Mock private VatDeclarationLineRepository vatDeclarationLineRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountingPeriodGuard periodGuard;
    @Mock private JournalEntryService journalEntryService;

    @InjectMocks
    private VatDeclarationService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");
        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void calculate_usesTaxableBaseAndPreviousCarriedForward() {
        Invoice sale = invoice(1L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        Invoice purchase = invoice(2L, InvoiceType.purchase, LocalDate.of(2026, 5, 11));
        VatPeriod previous = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(4)
                .carriedForwardVat(new BigDecimal("75.00"))
                .build();

        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.empty());
        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 4))
                .thenReturn(Optional.of(previous));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(sale, purchase));
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(line(2, "100.00", "10.00", "20.00", "999.00")));
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(2L, COMPANY_ID))
                .thenReturn(List.of(line(1, "50.00", null, "20.00", "60.00")));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(vatPeriodRepository.save(any())).thenAnswer(inv -> {
            VatPeriod p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(vatDeclarationLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        VatPeriodResponseDto result = service.calculate(new VatCalculateRequestDto(2026, 5, null));

        assertThat(result.totalOutputVat()).isEqualByComparingTo("36.00");
        assertThat(result.totalInputVat()).isEqualByComparingTo("10.00");
        assertThat(result.previousCarriedForwardVat()).isEqualByComparingTo("75.00");
        assertThat(result.netPayableVat()).isEqualByComparingTo("0.00");
        assertThat(result.carriedForwardVat()).isEqualByComparingTo("49.00");
        assertThat(result.lines()).anySatisfy(line ->
                assertThat(line.taxBase()).isEqualByComparingTo("180.00"));
    }

    @Test
    void calculate_whenVatPeriodLocked_throwsBusinessException() {
        VatPeriod locked = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .status("LOCKED")
                .build();
        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.calculate(new VatCalculateRequestDto(2026, 5, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("kilitli");

        verify(invoiceRepository, never()).findByCompanyCompanyIdAndIsDeletedFalse(anyLong());
    }

    @Test
    void lock_createsVatSettlementJournalAndStoresLockMetadata() {
        VatPeriod period = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .status("DRAFT")
                .totalOutputVat(new BigDecimal("100.00"))
                .totalInputVat(new BigDecimal("40.00"))
                .netPayableVat(new BigDecimal("60.00"))
                .build();
        period.setId(10L);

        when(vatPeriodRepository.findByIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(period));
        when(companyContext.getCurrentUserId()).thenReturn(77L);
        when(journalEntryService.createForVatSettlement(period)).thenReturn(500L);
        when(vatPeriodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VatPeriodResponseDto result = service.lock(10L);

        assertThat(result.status()).isEqualTo("LOCKED");
        assertThat(result.settlementJournalEntryId()).isEqualTo(500L);
        assertThat(result.lockedBy()).isEqualTo(77L);
        assertThat(result.lockedAt()).isNotNull();
        verify(periodGuard).assertOpen(LocalDate.of(2026, 5, 31));
    }

    @Test
    void getAll_returnsPeriodsWithLinesMappedToDtos() {
        VatPeriod period = period(10L, 2026, 5, "DRAFT");
        period.setLines(List.of(VatDeclarationLine.builder()
                .vatPeriod(period)
                .lineType("OUTPUT")
                .vatRate(20)
                .taxBase(new BigDecimal("100.00"))
                .vatAmount(new BigDecimal("20.00"))
                .build()));
        when(vatPeriodRepository.findByCompanyCompanyIdOrderByYearDescMonthDesc(COMPANY_ID))
                .thenReturn(List.of(period));

        List<VatPeriodResponseDto> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lines()).hasSize(1);
        assertThat(result.get(0).lines().get(0).vatAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void getById_whenPeriodExists_returnsDto() {
        VatPeriod period = period(10L, 2026, 5, "DRAFT");
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(Optional.of(period));

        VatPeriodResponseDto result = service.getById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @Test
    void getById_whenPeriodMissing_throws() {
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(99L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KDV");
    }

    @Test
    void calculate_updatesExistingPeriodAndUsesExplicitCarriedForward() {
        VatPeriod existing = period(10L, 2026, 5, "DRAFT");
        Invoice sale = invoice(1L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        Invoice ignoredDraft = invoice(2L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        ignoredDraft.setPaymentStatus(PaymentStatus.draft);
        Invoice ignoredCancelled = invoice(3L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        ignoredCancelled.setCancelled(true);
        Invoice ignoredOutsidePeriod = invoice(4L, InvoiceType.sale, LocalDate.of(2026, 6, 1));
        when(vatPeriodRepository.findByCompanyCompanyIdAndYearAndMonth(COMPANY_ID, 2026, 5))
                .thenReturn(Optional.of(existing));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(sale, ignoredDraft, ignoredCancelled, ignoredOutsidePeriod));
        InvoiceLineItem noVatLine = new InvoiceLineItem();
        noVatLine.setLineTotal(new BigDecimal("100.00"));
        noVatLine.setVatRate(BigDecimal.ZERO);
        when(invoiceLineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(1L, COMPANY_ID))
                .thenReturn(List.of(noVatLine));
        when(vatPeriodRepository.save(existing)).thenReturn(existing);
        when(vatDeclarationLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        VatPeriodResponseDto result = service.calculate(new VatCalculateRequestDto(2026, 5, new BigDecimal("12.345")));

        assertThat(result.previousCarriedForwardVat()).isEqualByComparingTo("12.35");
        assertThat(result.netPayableVat()).isEqualByComparingTo("0.00");
        assertThat(result.carriedForwardVat()).isEqualByComparingTo("12.35");
        verify(vatDeclarationLineRepository).deleteByVatPeriodId(10L);
        verify(vatDeclarationLineRepository).saveAll(any());
    }

    @Test
    void lock_whenAlreadyLocked_throwsBusinessException() {
        VatPeriod locked = period(10L, 2026, 5, "LOCKED");
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.lock(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zaten");

        verify(periodGuard, never()).assertOpen(any());
    }

    @Test
    void lock_whenPeriodMissing_throws() {
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(99L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lock(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KDV");
    }

    @Test
    void getBaBs_returnsOnlyCustomersAboveThresholdForSaleAndPurchase() {
        VatPeriod period = period(10L, 2026, 5, "DRAFT");
        Invoice sale = invoice(1L, InvoiceType.sale, LocalDate.of(2026, 5, 10));
        sale.setCustomerId(100L);
        sale.setTotalAmount(new BigDecimal("6000.00"));
        Invoice purchase = invoice(2L, InvoiceType.purchase, LocalDate.of(2026, 5, 11));
        purchase.setCustomerId(200L);
        purchase.setTotalAmount(new BigDecimal("7000.00"));
        Invoice belowThreshold = invoice(3L, InvoiceType.sale, LocalDate.of(2026, 5, 12));
        belowThreshold.setCustomerId(300L);
        belowThreshold.setTotalAmount(new BigDecimal("4999.99"));
        Invoice noCustomer = invoice(4L, InvoiceType.sale, LocalDate.of(2026, 5, 13));
        noCustomer.setTotalAmount(new BigDecimal("9999.00"));
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(10L, COMPANY_ID)).thenReturn(Optional.of(period));
        when(invoiceRepository.findByCompanyCompanyIdAndIsDeletedFalse(COMPANY_ID))
                .thenReturn(List.of(sale, purchase, belowThreshold, noCustomer));
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer(100L, "Alici A.S.", "1111111111")));
        when(customerRepository.findById(200L)).thenReturn(Optional.of(customer(200L, "Satici A.S.", "2222222222")));

        List<BaBsEntryDto> result = service.getBaBs(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BaBsEntryDto::formType).containsExactlyInAnyOrder("BS", "BA");
        assertThat(result).anySatisfy(row -> {
            assertThat(row.customerId()).isEqualTo(100L);
            assertThat(row.totalAmount()).isEqualByComparingTo("6000.00");
        });
    }

    @Test
    void getBaBs_whenPeriodMissing_throws() {
        when(vatPeriodRepository.findByIdAndCompanyCompanyId(99L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBaBs(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KDV");
    }

    private Invoice invoice(Long id, InvoiceType type, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setCompany(company);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(date);
        invoice.setPaymentStatus(PaymentStatus.pending);
        invoice.setCancelled(false);
        return invoice;
    }

    private InvoiceLineItem line(int quantity, String unitPrice, String discountRate, String vatRate, String lineTotal) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setDiscountRate(discountRate != null ? new BigDecimal(discountRate) : null);
        line.setVatRate(new BigDecimal(vatRate));
        line.setLineTotal(new BigDecimal(lineTotal));
        return line;
    }

    private VatPeriod period(Long id, int year, int month, String status) {
        VatPeriod period = VatPeriod.builder()
                .company(company)
                .year(year)
                .month(month)
                .status(status)
                .totalOutputVat(new BigDecimal("100.00"))
                .totalInputVat(new BigDecimal("40.00"))
                .netPayableVat(new BigDecimal("60.00"))
                .previousCarriedForwardVat(BigDecimal.ZERO)
                .carriedForwardVat(BigDecimal.ZERO)
                .lines(List.of())
                .build();
        period.setId(id);
        return period;
    }

    private Customer customer(Long id, String name, String taxNumber) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setName(name);
        customer.setTaxNumber(taxNumber);
        return customer;
    }
}
