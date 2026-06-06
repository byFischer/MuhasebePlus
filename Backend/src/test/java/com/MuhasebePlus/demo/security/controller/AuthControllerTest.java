package com.MuhasebePlus.demo.security.controller;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired WebApplicationContext wac;

    @MockitoBean AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ── /api/auth/login public endpoint ──────────────────────────────────────

    @Test
    void login_publicEndpoint_returns200() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponseDto("jwt-token-abc"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-abc"));
    }

    @Test
    void login_noAuthorizationHeaderRequired_returns200() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponseDto("token-xyz"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().isOk());
    }
}
