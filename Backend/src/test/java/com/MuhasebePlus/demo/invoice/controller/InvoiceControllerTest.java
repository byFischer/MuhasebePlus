package com.MuhasebePlus.demo.invoice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class InvoiceControllerTest {

    @MockitoBean
    InvoiceService invoiceService;

    @Autowired
    WebApplicationContext context;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createInvoice_ValidBody_ShouldReturn201() throws Exception {
        // Arrange
        InvoiceRequestDto requestDto = new InvoiceRequestDto(
        "INV-001", 10L, InvoiceType.sale,
        LocalDate.of(2026, 6, 1), PaymentStatus.pending,
        new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
    );

    InvoiceResponseDto responseDto = new InvoiceResponseDto(1L, "INV-001", 10L, "sale",
        LocalDate.of(2026, 6, 1), "pending", new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"));

        when(invoiceService.createInvoice(any())).thenReturn(responseDto);

        // Act & Assert

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
            .andExpect(jsonPath("$.paymentStatus").value("pending"));

            verify(invoiceService).createInvoice(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createInvoice_InvalidBody_ShouldReturn400() throws Exception {
        InvoiceRequestDto requestDto = new InvoiceRequestDto(
            "", 10L, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.pending,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")

        ); 

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createInvoice_NullCustomerId_ShouldReturn400() throws Exception {
        InvoiceRequestDto requestDto = new InvoiceRequestDto(
            "INV-001", null, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.pending,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")

        ); 

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createInvoice_NoAuth_ShouldReturn401() throws Exception {
        InvoiceRequestDto requestDto = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.pending,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );

        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllInvoices_ShouldReturn200() throws Exception {
        when(invoiceService.getInvoiceByFilters(any(), any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/invoices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getInvoiceById_ShouldReturn200() throws Exception {
        InvoiceResponseDto responseDto = new InvoiceResponseDto(
            1L, "INV-001", 10L, "sale",
            LocalDate.of(2026, 6, 1), "pending",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        when(invoiceService.getInvoiceById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/invoices/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateInvoice_ValidBody_ShouldReturn200() throws Exception {
        InvoiceRequestDto requestDto = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.paid,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        InvoiceResponseDto responseDto = new InvoiceResponseDto(
            1L, "INV-001", 10L, "sale",
            LocalDate.of(2026, 6, 1), "paid",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        when(invoiceService.updateInvoice(any(), any())).thenReturn(responseDto);

        mockMvc.perform(put("/api/invoices/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("paid"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteInvoice_Admin_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/invoices/1"))
            .andExpect(status().isNoContent());

        verify(invoiceService).deleteInvoiceById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteInvoice_User_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/invoices/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getInvoicesByCustomerId_ShouldReturn200() throws Exception {
        when(invoiceService.getInvoiceByCustomerId(10L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/invoices/customer/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updatePaymentStatus_ShouldReturn200() throws Exception {
        InvoiceResponseDto responseDto = new InvoiceResponseDto(
            1L, "INV-001", 10L, "sale",
            LocalDate.of(2026, 6, 1), "paid",
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        when(invoiceService.updatePaymentStatus(1L, PaymentStatus.paid)).thenReturn(responseDto);

        mockMvc.perform(put("/api/invoices/1/status")
                .param("status", "paid"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("paid"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getInvoiceById_ServiceException_ShouldReturn500() throws Exception {
        when(invoiceService.getInvoiceById(999L)).thenThrow(new RuntimeException("Invoice not found with id: 999"));

        mockMvc.perform(get("/api/invoices/999"))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updatePaymentStatus_InvalidEnum_ShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/invoices/1/status")
                .param("status", "invalid"))
            .andExpect(status().isBadRequest());
    }
}
