package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceLog;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.repository.EInvoiceLogRepository;
import com.MuhasebePlus.demo.invoice.repository.EInvoiceProfileRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EInvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineItemRepository lineItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EInvoiceProfileRepository profileRepository;
    @Mock private EInvoiceLogRepository logRepository;
    @Mock private GibClient gibClient;
    @Mock private CompanyContext companyContext;
    @Mock private SystemLogService systemLogService;

    private EInvoiceService service;
    private Company company;
    private EInvoiceProfile profile;

    @BeforeEach
    void setUp() {
        service = new EInvoiceService(
                invoiceRepository,
                lineItemRepository,
                customerRepository,
                productRepository,
                companyRepository,
                profileRepository,
                logRepository,
                gibClient,
                companyContext,
                systemLogService
        );
        company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        company.setTaxNumber("1234567890");
        profile = EInvoiceProfile.builder()
                .company(company)
                .gibAlias("alias")
                .gibPassword("secret")
                .isActive(true)
                .build();
        org.mockito.Mockito.lenient().when(companyContext.getCurrentCompanyId()).thenReturn(1L);
    }

    @Test
    void sendToGib_whenGibAcceptsInvoice_persistsSentLog() {
        Invoice invoice = invoice(100L);
        Customer customer = customer();
        InvoiceLineItem line = line(10);
        Product product = Product.builder().productId(10).name("Hizmet").build();
        GibClient.GibResponse gibResponse = new GibClient.GibResponse();
        gibResponse.success = true;
        gibResponse.ettin = "ETTN-1";
        gibResponse.message = "DEMO MODU - OK";
        gibResponse.rawResponse = "<ok/>";
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(customer));
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, 1L)).thenReturn(List.of(line, line));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(10, 1L)).thenReturn(Optional.of(product));
        when(logRepository.save(any(EInvoiceLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gibClient.sendInvoice(any(EInvoiceProfile.class), any(String.class))).thenReturn(gibResponse);

        EInvoiceLog result = service.sendToGib(100L);

        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getEnvelopeId()).isEqualTo("ETTN-1");
        assertThat(result.getUuid()).isNotBlank();
        assertThat(result.getRequestBody()).contains("<cbc:ID>FTR-100</cbc:ID>");
        verify(logRepository, times(2)).save(any(EInvoiceLog.class));
        verify(productRepository, times(1)).findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(10, 1L);
    }

    @Test
    void sendToGib_whenGibRejectsInvoice_marksLogFailedAndThrows() {
        Invoice invoice = invoice(100L);
        GibClient.GibResponse gibResponse = new GibClient.GibResponse();
        gibResponse.success = false;
        gibResponse.message = "Red";
        gibResponse.rawResponse = "<error/>";
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(customerRepository.findByCustomerIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.empty());
        when(lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(100L, 1L)).thenReturn(List.of(line(10)));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(10, 1L)).thenReturn(Optional.empty());
        when(logRepository.save(any(EInvoiceLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gibClient.sendInvoice(any(EInvoiceProfile.class), any(String.class))).thenReturn(gibResponse);

        assertThatThrownBy(() -> service.sendToGib(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Red");
    }

    @Test
    void sendToGib_whenRequiredDataMissing_throwsBeforeSending() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendToGib(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void checkGibStatus_updatesLastLogWithResponse() {
        EInvoiceLog lastLog = new EInvoiceLog();
        lastLog.setInvoiceId(100L);
        lastLog.setEnvelopeId("ETTN-1");
        GibClient.GibResponse response = new GibClient.GibResponse();
        response.success = false;
        response.message = "Beklemede";
        response.rawResponse = "<status/>";
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));
        when(logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(100L, 1L)).thenReturn(List.of(lastLog));
        when(gibClient.checkStatus(profile, "ETTN-1")).thenReturn(response);
        when(logRepository.save(lastLog)).thenReturn(lastLog);

        EInvoiceLog result = service.checkGibStatus(100L);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).isEqualTo("Beklemede");
        assertThat(result.getResponseBody()).isEqualTo("<status/>");
    }

    @Test
    void checkGibStatus_whenNoEnvelopeExists_throws() {
        EInvoiceLog lastLog = new EInvoiceLog();
        lastLog.setEnvelopeId(" ");
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));
        when(logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(100L, 1L)).thenReturn(List.of(lastLog));

        assertThatThrownBy(() -> service.checkGibStatus(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("önce");
    }

    @Test
    void checkGibStatus_whenNoLogExists_throws() {
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));
        when(logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(100L, 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkGibStatus(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("kaydı");
    }

    @Test
    void getLogsAndProfileUseCurrentCompany() {
        EInvoiceLog log = new EInvoiceLog();
        when(logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(100L, 1L)).thenReturn(List.of(log));
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(profile));

        assertThat(service.getLogs(100L)).containsExactly(log);
        assertThat(service.getProfile()).isSameAs(profile);
    }

    @Test
    void saveProfile_deactivatesExistingProfileAndCreatesNewActiveProfile() {
        EInvoiceProfile existing = EInvoiceProfile.builder().isActive(true).build();
        when(profileRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(profileRepository.save(any(EInvoiceProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        EInvoiceProfile result = service.saveProfile(1L, "new-alias", "new-secret", "ARS");

        assertThat(existing.isActive()).isFalse();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getGibAlias()).isEqualTo("new-alias");
        assertThat(result.getDefaultSeriesCode()).isEqualTo("ARS");
        verify(profileRepository, times(2)).save(any(EInvoiceProfile.class));
    }

    private Invoice invoice(Long id) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setInvoiceNumber("FTR-" + id);
        invoice.setCustomerId(7L);
        invoice.setInvoiceType(InvoiceType.sale);
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 10));
        invoice.setDueDate(LocalDate.of(2026, 1, 25));
        invoice.setTotalAmount(new BigDecimal("120.00"));
        return invoice;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setCustomerId(7L);
        customer.setName("ABC Ltd");
        customer.setType(CustomerType.CORPORATE);
        customer.setTaxNumber("9876543210");
        return customer;
    }

    private InvoiceLineItem line(Integer productId) {
        InvoiceLineItem line = new InvoiceLineItem();
        line.setProductId(productId);
        line.setQuantity(1);
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setVatRate(new BigDecimal("20.00"));
        return line;
    }
}
