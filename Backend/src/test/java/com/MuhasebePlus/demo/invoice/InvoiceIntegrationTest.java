package com.MuhasebePlus.demo.invoice;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class InvoiceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineItemRepository invoiceLineItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private String userToken;
    private String adminToken;

    private Long customerId;
    private Integer productId;

    private InvoiceRequestDto validRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        invoiceLineItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        stockRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
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

        Customer customer = new Customer();
        customer.setName("ACME Ltd.");
        customer.setTaxNumber("TAX-001");
        customer.setCity("Istanbul");
        customer.setType(CustomerType.CORPORATE);
        customer.setCurrentBalance(BigDecimal.ZERO);
        customer.setDeleted(false);
        customerId = customerRepository.save(customer).getCustomerId();

        Product product = new Product();
        product.setBarcode("BAR-1");
        product.setName("Widget");
        product.setUnit("adet");
        product.setSalePrice(new BigDecimal("10.00"));
        product.setVatRate(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("8.00"));
        product.setDeleted(false);
        productId = productRepository.save(product).getProductId();

        Stock stock = new Stock();
        stock.setProductId(productId);
        stock.setQuantity(100);
        stock.setMinQuantity(5);
        stock.setDeleted(false);
        stockRepository.save(stock);

        validRequest = new InvoiceRequestDto(
                "INV-001",
                customerId,
                InvoiceType.sale,
                LocalDate.of(2026, 6, 1),
                List.of(new InvoiceLineItemRequestDto(productId, 10))
        );
    }

    @Test
    void fullLifecycle_CreateGetUpdateStatusDelete() throws Exception {
        String createResponse = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.paymentStatus").value("pending"))
                .andExpect(jsonPath("$.lineItems.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long invoiceId = objectMapper.readTree(createResponse).get("invoiceId").asLong();

        mockMvc.perform(get("/api/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"));

        InvoiceRequestDto updateRequest = new InvoiceRequestDto(
                "INV-001",
                customerId,
                InvoiceType.sale,
                LocalDate.of(2026, 12, 1),
                List.of(new InvoiceLineItemRequestDto(productId, 10))
        );

        mockMvc.perform(put("/api/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));

        mockMvc.perform(put("/api/invoices/" + invoiceId + "/payment-status")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("paid"));

        mockMvc.perform(get("/api/invoices?paymentStatus=paid&invoiceType=sale")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Paid fatura silinemez
        mockMvc.perform(delete("/api/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is5xxServerError());
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
                .andReturn()
                .getResponse()
                .getContentAsString();

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
    void saleInvoice_SufficientStock_ShouldDecreaseStockAndPersistEnums() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        Invoice saved = invoiceRepository.findByInvoiceNumber("INV-001").orElseThrow();
        assertThat(saved.getInvoiceType()).isEqualTo(InvoiceType.sale);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.pending);

        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(90);
    }

    @Test
    void saleInvoice_InsufficientStock_ShouldBeDraftAndNotTouchStock() throws Exception {
        InvoiceRequestDto bigRequest = new InvoiceRequestDto(
                "INV-BIG",
                customerId,
                InvoiceType.sale,
                LocalDate.of(2026, 6, 1),
                List.of(new InvoiceLineItemRequestDto(productId, 9999))
        );

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bigRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("draft"));

        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(100);
    }

    @Test
    void confirmInvoice_DraftWithStock_ShouldTransitionToPending() throws Exception {
        InvoiceRequestDto bigRequest = new InvoiceRequestDto(
                "INV-DRAFT",
                customerId,
                InvoiceType.sale,
                LocalDate.of(2026, 6, 1),
                List.of(new InvoiceLineItemRequestDto(productId, 9999))
        );

        String draftResponse = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bigRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("draft"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long draftId = objectMapper.readTree(draftResponse).get("invoiceId").asLong();

        // Stok yeterli hale gelsin
        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
        stock.setQuantity(20000);
        stockRepository.save(stock);

        mockMvc.perform(put("/api/invoices/" + draftId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("pending"));

        Stock after = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(after.getQuantity()).isEqualTo(20000 - 9999);
    }

    @Test
    void filterCombination_ShouldReturnCorrectSubset() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        InvoiceRequestDto purchaseRequest = new InvoiceRequestDto(
                "INV-002",
                customerId,
                InvoiceType.purchase,
                LocalDate.of(2026, 7, 1),
                List.of(new InvoiceLineItemRequestDto(productId, 5))
        );

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/invoices?invoiceType=sale")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].invoiceType").value("sale"));

        mockMvc.perform(get("/api/invoices")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
