package com.MuhasebePlus.demo.invoice;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class InvoiceIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtUtil jwtUtil;
    @Autowired PasswordEncoder passwordEncoder;

    MockMvc mockMvc;

    String userToken;
    String adminToken;

    InvoiceRequestDto validRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        invoiceRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        userRepository.save(user);

        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        userToken = jwtUtil.generateToken(user);
        adminToken = jwtUtil.generateToken(admin);

        validRequest = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.pending,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
    }

    @Test
    void fullLifecycle_CreateGetUpdateStatusDelete() throws Exception {
        // Create
        String createResponse = mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
            .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(createResponse).get("invoiceId").asLong();

        // Get by ID
        mockMvc.perform(get("/api/invoices/" + invoiceId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));

        // Update
        InvoiceRequestDto updateRequest = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale,
            LocalDate.of(2026, 6, 1), PaymentStatus.paid,
            new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        mockMvc.perform(put("/api/invoices/" + invoiceId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("paid"));

        // Update status
        mockMvc.perform(put("/api/invoices/" + invoiceId + "/status")
                .header("Authorization", "Bearer " + userToken)
                .param("status", "overdue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("overdue"));

        // Filter
        mockMvc.perform(get("/api/invoices?status=overdue&type=sale")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // Delete (ADMIN)
        mockMvc.perform(delete("/api/invoices/" + invoiceId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void createInvoice_DuplicateNumber_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().is5xxServerError());
    }

    @Test
    void deleteInvoice_UserRole_ShouldReturn403() throws Exception {
        String response = mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long invoiceId = objectMapper.readTree(response).get("invoiceId").asLong();

        mockMvc.perform(delete("/api/invoices/" + invoiceId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void createInvoice_NoToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void enumRoundTrip_InvoiceTypeShouldBeLowerCaseInDb() throws Exception {
        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated());

        Invoice saved = invoiceRepository.findByInvoiceNumber("INV-001").orElseThrow();
        assertThat(saved.getInvoiceType()).isEqualTo(InvoiceType.sale);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.pending);
    }

    @Test
    void filterCombination_ShouldReturnCorrectSubset() throws Exception {
        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isCreated());

        InvoiceRequestDto purchaseRequest = new InvoiceRequestDto(
            "INV-002", 10L, InvoiceType.purchase,
            LocalDate.of(2026, 7, 1), PaymentStatus.paid,
            new BigDecimal("200.00"), new BigDecimal("40.00"), new BigDecimal("240.00")
        );
        mockMvc.perform(post("/api/invoices")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(purchaseRequest)))
            .andExpect(status().isCreated());

        // sadece sale tipi → 1 sonuç
        mockMvc.perform(get("/api/invoices?type=sale")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].invoiceType").value("sale"));

        // tümü → 2 sonuç
        mockMvc.perform(get("/api/invoices")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
