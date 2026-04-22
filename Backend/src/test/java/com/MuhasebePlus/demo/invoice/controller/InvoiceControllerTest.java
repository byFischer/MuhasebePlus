package com.MuhasebePlus.demo.invoice.controller;

import com.MuhasebePlus.demo.common.exception.GlobalExceptionHandler;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceControllerTest {

    @Mock
    InvoiceService invoiceService;

    @InjectMocks
    InvoiceController invoiceController;

    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    InvoiceRequestDto validRequest;
    InvoiceResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
            .setValidator(validator)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        validRequest = new InvoiceRequestDto(
            "INV-001",
            10L,
            InvoiceType.sale,
            LocalDate.of(2026, 6, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        sampleResponse = new InvoiceResponseDto(
            1L,
            "INV-001",
            10L,
            "ACME Ltd.",
            "sale",
            LocalDate.of(2026, 6, 1),
            "pending",
            new BigDecimal("100.00"),
            new BigDecimal("20.00"),
            new BigDecimal("120.00"),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Test
    void createInvoice_ValidBody_ShouldReturn201() throws Exception {
        when(invoiceService.createInvoice(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
            .andExpect(jsonPath("$.paymentStatus").value("pending"));

        verify(invoiceService).createInvoice(any());
    }

    @Test
    void createInvoice_BlankInvoiceNumber_ShouldReturn400() throws Exception {
        InvoiceRequestDto invalid = new InvoiceRequestDto(
            "",
            10L,
            InvoiceType.sale,
            LocalDate.of(2026, 6, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createInvoice_NullCustomerId_ShouldReturn400() throws Exception {
        InvoiceRequestDto invalid = new InvoiceRequestDto(
            "INV-001",
            null,
            InvoiceType.sale,
            LocalDate.of(2026, 6, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createInvoice_EmptyLineItems_ShouldReturn400() throws Exception {
        InvoiceRequestDto invalid = new InvoiceRequestDto(
            "INV-001",
            10L,
            InvoiceType.sale,
            LocalDate.of(2026, 6, 1),
            List.of()
        );

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getInvoices_ShouldReturn200() throws Exception {
        when(invoiceService.getInvoiceByFilters(any(), any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/invoices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getInvoiceById_ShouldReturn200() throws Exception {
        when(invoiceService.getInvoiceById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/invoices/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    void getInvoiceById_ServiceException_ShouldReturn500() throws Exception {
        when(invoiceService.getInvoiceById(999L)).thenThrow(new RuntimeException("Invoice not found with id: 999"));

        mockMvc.perform(get("/api/invoices/999"))
            .andExpect(status().is5xxServerError());
    }

    @Test
    void updateInvoice_ValidBody_ShouldReturn200() throws Exception {
        InvoiceResponseDto updatedResponse = new InvoiceResponseDto(
            1L, "INV-001", 10L, "ACME Ltd.", "sale",
            LocalDate.of(2026, 12, 1), "pending",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"),
            List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(invoiceService.updateInvoice(eq(1L), any())).thenReturn(updatedResponse);

        InvoiceRequestDto updateRequest = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale, LocalDate.of(2026, 12, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        mockMvc.perform(put("/api/invoices/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dueDate").value("2026-12-01"));
    }

    @Test
    void updateInvoice_InvalidBody_ShouldReturn400() throws Exception {
        InvoiceRequestDto invalid = new InvoiceRequestDto(
            "", null, InvoiceType.sale, LocalDate.of(2026, 6, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        mockMvc.perform(put("/api/invoices/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteInvoice_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/invoices/1"))
            .andExpect(status().isNoContent());

        verify(invoiceService).deleteInvoice(1L);
    }

    @Test
    void getInvoicesByCustomerId_ShouldReturn200() throws Exception {
        when(invoiceService.getInvoiceByCustomerId(10L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/invoices/customer/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void confirmInvoice_ShouldReturn200() throws Exception {
        InvoiceResponseDto confirmed = new InvoiceResponseDto(
            1L, "INV-001", 10L, "ACME Ltd.", "sale",
            LocalDate.of(2026, 6, 1), "pending",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"),
            List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(invoiceService.confirmInvoice(1L)).thenReturn(confirmed);

        mockMvc.perform(put("/api/invoices/1/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("pending"));
    }

    @Test
    void updatePaymentStatus_ShouldReturn200() throws Exception {
        InvoiceResponseDto paidResponse = new InvoiceResponseDto(
            1L, "INV-001", 10L, "ACME Ltd.", "sale",
            LocalDate.of(2026, 6, 1), "paid",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"),
            List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(invoiceService.updatePaymentStatus(1L, PaymentStatus.paid)).thenReturn(paidResponse);

        mockMvc.perform(put("/api/invoices/1/payment-status")
                .param("status", "paid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("paid"));
    }

    @Test
    void updatePaymentStatus_InvalidEnum_ShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/invoices/1/payment-status")
                .param("status", "invalid"))
            .andExpect(status().isBadRequest());
    }
}
