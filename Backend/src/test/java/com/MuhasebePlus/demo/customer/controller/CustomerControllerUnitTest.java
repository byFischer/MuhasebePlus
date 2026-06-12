package com.MuhasebePlus.demo.customer.controller;

import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerActivityDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerAgingDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.ImportResultDto;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.service.CustomerExcelService;
import com.MuhasebePlus.demo.customer.service.CustomerPdfService;
import com.MuhasebePlus.demo.customer.service.CustomerService;
import com.MuhasebePlus.demo.financial.entity.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerUnitTest {

    @Mock private CustomerService customerService;
    @Mock private CustomerExcelService customerExcelService;
    @Mock private CustomerPdfService customerPdfService;

    @InjectMocks
    private CustomerController controller;

    @Test
    void customerCrudAndQueryEndpoints_delegateToService() {
        CustomerRequestDto request = request();
        CustomerResponseDto response = response(10L, "ABC Ltd");
        PageRequest pageable = PageRequest.of(0, 20);
        Page<CustomerResponseDto> page = new PageImpl<>(List.of(response), pageable, 1);
        when(customerService.createCustomer(request)).thenReturn(response);
        when(customerService.searchCustomersPaged("abc", pageable)).thenReturn(page);
        when(customerService.getCustomersByTypePaged("CORPORATE", pageable)).thenReturn(page);
        when(customerService.getAllCustomersPaged(pageable)).thenReturn(page);
        when(customerService.getCustomerById(10L)).thenReturn(response);
        when(customerService.updateCustomer(10L, request)).thenReturn(response);
        when(customerService.restoreCustomer(10L)).thenReturn(response);

        assertThat(controller.createCustomer(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getAllCustomers("abc", null, pageable).getBody()).isSameAs(page);
        assertThat(controller.getAllCustomers(null, "CORPORATE", pageable).getBody()).isSameAs(page);
        assertThat(controller.getAllCustomers(" ", " ", pageable).getBody()).isSameAs(page);
        assertThat(controller.getCustomerById(10L).getBody()).isSameAs(response);
        assertThat(controller.updateCustomer(10L, request).getBody()).isSameAs(response);
        assertThat(controller.deleteCustomer(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.restoreCustomer(10L).getBody()).isSameAs(response);
        verify(customerService).softDeleteCustomer(10L);
    }

    @Test
    void noteImportActivityAgingAndExportEndpoints_delegateToServices() {
        CustomerNoteRequestDto noteRequest = new CustomerNoteRequestDto("Aranacak");
        CustomerNoteResponseDto noteResponse = new CustomerNoteResponseDto(55L, 10L, "Aranacak", null, null);
        CustomerActivityDto activity = new CustomerActivityDto(
                LocalDate.of(2026, 1, 1), "FATURA", "Satis", "FTR-1",
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        CustomerAgingDto aging = new CustomerAgingDto(
                10L, "ABC Ltd", "120.001", new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        ImportResultDto importResult = new ImportResultDto(1, 0, List.of());
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv", "text/csv", new byte[]{1});
        byte[] excel = new byte[]{1, 2, 3};
        byte[] pdf = new byte[]{4, 5};
        when(customerService.addNote(10L, noteRequest)).thenReturn(noteResponse);
        when(customerService.getNotesByCustomerId(10L)).thenReturn(List.of(noteResponse));
        when(customerService.updateNote(55L, noteRequest)).thenReturn(noteResponse);
        when(customerService.importCustomers(file)).thenReturn(importResult);
        when(customerService.getCustomerActivity(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(activity));
        when(customerService.getCustomerAging()).thenReturn(List.of(aging));
        when(customerExcelService.exportToExcel()).thenReturn(excel);
        when(customerPdfService.exportToPdf()).thenReturn(pdf);

        assertThat(controller.addNote(10L, noteRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getNotesByCustomerId(10L).getBody()).containsExactly(noteResponse);
        assertThat(controller.updateNote(10L, 55L, noteRequest).getBody()).isSameAs(noteResponse);
        assertThat(controller.deleteNote(10L, 55L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.importCustomers(file).getBody()).isSameAs(importResult);
        assertThat(controller.getActivity(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).getBody())
                .containsExactly(activity);
        assertThat(controller.getAging().getBody()).containsExactly(aging);
        assertThat(controller.exportExcel().getHeaders().getContentLength()).isEqualTo(3);
        assertThat(controller.exportExcel().getHeaders().getContentType().toString()).contains("spreadsheetml");
        assertThat(controller.exportPdf().getHeaders().getContentLength()).isEqualTo(2);
        assertThat(controller.exportPdf().getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        verify(customerService).deleteNote(55L);
    }

    private CustomerRequestDto request() {
        return new CustomerRequestDto(
                "ABC Ltd",
                "1234567890",
                "Adres",
                "Istanbul",
                "5321234567",
                CustomerType.CORPORATE,
                "abc@example.com",
                "120.001",
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                "Kadikoy",
                null,
                "TR123",
                Currency.TRY,
                new BigDecimal("1000.00"),
                CustomerRole.BUYER,
                CustomerStatus.ACTIVE,
                "A Grubu"
        );
    }

    private CustomerResponseDto response(Long id, String name) {
        return new CustomerResponseDto(
                id,
                name,
                "abc@example.com",
                "1234567890",
                "Adres",
                "Istanbul",
                "5321234567",
                "CORPORATE",
                BigDecimal.ZERO,
                false,
                false,
                null,
                null,
                "120.001",
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                "Kadikoy",
                null,
                "TR123",
                "TRY",
                new BigDecimal("1000.00"),
                "BUYER",
                "ACTIVE",
                "A Grubu"
        );
    }
}
