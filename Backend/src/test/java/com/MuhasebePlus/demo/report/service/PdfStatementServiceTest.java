package com.MuhasebePlus.demo.report.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.response.CustomerActivityDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.customer.service.CustomerService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfStatementServiceTest {

    @Mock
    private CustomerService customerService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyContext companyContext;

    @Test
    void generatePdf_writesCompanyCustomerActivitiesAndTotals() throws Exception {
        PdfStatementService service = new PdfStatementService(
                customerService,
                customerRepository,
                companyRepository,
                companyContext
        );
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        Customer customer = customer();
        Company company = company();
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(7L, 1L))
                .thenReturn(Optional.of(customer));
        when(customerService.getCustomerActivity(7L, start, end))
                .thenReturn(List.of(
                        new CustomerActivityDto(
                                LocalDate.of(2026, 5, 3),
                                "Fatura",
                                "Satis faturasi aciklamasi uzun",
                                "S-100",
                                new BigDecimal("500.00"),
                                BigDecimal.ZERO,
                                new BigDecimal("600.00")
                        ),
                        new CustomerActivityDto(
                                LocalDate.of(2026, 5, 8),
                                "Tahsilat",
                                null,
                                null,
                                BigDecimal.ZERO,
                                new BigDecimal("150.00"),
                                new BigDecimal("450.00")
                        )
                ));

        byte[] pdf = service.generatePdf(7L, start, end);

        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("CARI HESAP EKSTRESI");
            assertThat(text).contains("Test A.S.");
            assertThat(text).contains("Ada Ltd.");
            assertThat(text).contains("Kapanis Bakiyesi");
        }
    }

    @Test
    void generatePdf_whenCustomerMissing_throwsBeforePdfCreation() {
        PdfStatementService service = new PdfStatementService(
                customerService,
                customerRepository,
                companyRepository,
                companyContext
        );
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePdf(99L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setCustomerId(7L);
        customer.setName("Ada Ltd.");
        customer.setTaxNumber("1234567890");
        customer.setTaxOffice("Kadikoy");
        customer.setAccountCode("120.01.001");
        customer.setOpeningBalance(new BigDecimal("100.00"));
        return customer;
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        return company;
    }
}
