package com.MuhasebePlus.demo.customer.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerExportServiceTest {

    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private CustomerService customerService;

    @Test
    void exportToExcel_writesCustomerHeadersAndRows() throws Exception {
        CustomerExcelService service = new CustomerExcelService(customerService);
        when(customerService.getAllCustomers()).thenReturn(List.of(response(10L, "ABC Ltd", true)));

        byte[] data = service.exportToExcel();

        assertThat(data).isNotEmpty();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getSheet("Musteri Listesi");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Hesap Kodu");
            assertThat(sheet.getRow(0).getCell(19).getStringCellValue()).isEqualTo("Vadesi Gecmis");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("ABC Ltd");
            assertThat(sheet.getRow(1).getCell(12).getNumericCellValue()).isEqualTo(100.50);
            assertThat(sheet.getRow(1).getCell(15).getNumericCellValue()).isEqualTo(250.75);
            assertThat(sheet.getRow(1).getCell(19).getStringCellValue()).isEqualTo("Evet");
        }
    }

    @Test
    void exportToPdf_writesCompanyTitleAndPaginatesCustomerRows() throws Exception {
        CustomerPdfService service = new CustomerPdfService(companyContext, companyRepository, customerService);
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        List<CustomerResponseDto> customers = IntStream.rangeClosed(1, 55)
                .mapToObj(i -> response((long) i, "Uzun Musteri Adi " + i + " Ek Bilgi", i % 2 == 0))
                .toList();
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(customerService.getAllCustomers()).thenReturn(customers);

        byte[] data = service.exportToPdf();

        assertThat(data).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(data)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(text).contains("MUSTERI LISTESI - Test A.S.");
            assertThat(text).contains("Toplam: 55 musteri");
        }
    }

    private CustomerResponseDto response(Long id, String name, boolean overdue) {
        return new CustomerResponseDto(
                id,
                name,
                "musteri" + id + "@example.com",
                "1234567890",
                "Adres",
                "Istanbul",
                "5321234567",
                "CORPORATE",
                new BigDecimal("250.75"),
                overdue,
                false,
                null,
                null,
                "120." + String.format("%03d", id),
                new BigDecimal("100.50"),
                LocalDate.of(2026, 1, 1),
                "Kadikoy",
                "11111111111",
                "TR123456789012345678901234",
                "TRY",
                new BigDecimal("5000.00"),
                "BUYER",
                "ACTIVE",
                "A Grubu"
        );
    }
}
