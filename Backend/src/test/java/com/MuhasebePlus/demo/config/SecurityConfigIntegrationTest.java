package com.MuhasebePlus.demo.config;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    private static final String TEST_EMAIL = "securityconfig@test.com";
    private static final String TEST_PASSWORD = "secret123";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired WebApplicationContext wac;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);

        User user = new User();
        user.setEmail(TEST_EMAIL);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setFirstName("Security");
        user.setLastName("Config");
        user.setRole(UserRole.USER);
        userRepository.save(user);
    }

    // ── Anonim istekler — korunan endpoint'ler 401 ────────────────────────────

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/journal-entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customersEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutAuthPost_returns401() throws Exception {
        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    // ── Rol bazlı yetkilendirme — /api/admin/** ──────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void adminStats_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminUsers_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminStats_withAdminRole_isNotRejectedByAuthorization() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/stats")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    @WithMockUser(roles = "USER")
    void protectedEndpoint_withUserRole_isNotRejectedByAuthorization() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/journal-entries")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ── Açık (permitAll) endpoint'ler ─────────────────────────────────────────

    @Test
    void login_withoutAuth_isReachableAndReturnsToken() throws Exception {
        String body = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("token");
    }

    @Test
    void publicShareEndpoint_withoutAuth_isNotRejectedByAuthorization() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/share/nonexistent-token-123")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void actuatorHealth_withoutAuth_isNotRejectedByAuthorization() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void registerEndpoint_withoutAuth_isNotRejectedByAuthorization() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ── CORS preflight ────────────────────────────────────────────────────────

    @Test
    void corsPreflight_fromAllowedOrigin_returnsCorsHeaders() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/journal-entries")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void corsPreflight_fromDisallowedOrigin_returnsForbidden() throws Exception {
        mockMvc.perform(options("/api/journal-entries")
                        .header("Origin", "http://malicious.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
