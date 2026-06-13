package com.MuhasebePlus.demo.accounting.controller;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.accounting.dto.response.JournalEntryResponseDto;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.accounting.service.JournalEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class JournalEntryControllerTest {

    @Autowired WebApplicationContext wac;

    @MockitoBean JournalEntryService journalEntryService;

    private MockMvc mockMvc;
    private static final String BASE_URL = "/api/journal-entries";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ── 401 – kimlik doğrulama olmadan ────────────────────────────────────────

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── 200 – USER rolü ile okuma ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void list_withUserRole_returns200() throws Exception {
        Page<JournalEntryResponseDto> emptyPage = new PageImpl<>(List.of());
        when(journalEntryService.list(any())).thenReturn(emptyPage);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getById_withUserRole_returns200() throws Exception {
        JournalEntryResponseDto dto = new JournalEntryResponseDto(
                1L, "FIS-2025-0001", LocalDate.now(), "Test",
                JournalSourceType.MANUAL, null, false, null, List.of(), LocalDateTime.now());
        when(journalEntryService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(1));
    }

    // ── 403 – USER rolü yazma yapamaz ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void create_withUserRole_returns403() throws Exception {
        String body = """
                {
                  "entryDate": "2025-01-15",
                  "description": "Test",
                  "lines": [
                    {"accountId": 1, "debitAmount": 100.00, "creditAmount": 0.00}
                  ]
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_withUserRole_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isForbidden());
    }

    // ── 201 – ADMIN rolü kayıt oluşturabilir ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withAdminRole_validBody_returns201() throws Exception {
        JournalEntryResponseDto saved = new JournalEntryResponseDto(
                10L, "FIS-2025-0001", LocalDate.of(2025, 1, 15), "Test",
                JournalSourceType.MANUAL, null, false, null, List.of(), LocalDateTime.now());
        when(journalEntryService.createManualEntry(any())).thenReturn(saved);

        String body = """
                {
                  "entryDate": "2025-01-15",
                  "description": "Test",
                  "lines": [
                    {"accountId": 1, "debitAmount": 100.00, "creditAmount": 0.00}
                  ]
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryId").value(10));
    }

    // ── 400 – boş lines listesi validation hatası ─────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withAdminRole_emptyLines_returns400() throws Exception {
        String body = """
                {
                  "entryDate": "2025-01-15",
                  "description": "Test",
                  "lines": []
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── 204 – ADMIN silme ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returns204() throws Exception {
        doNothing().when(journalEntryService).delete(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }
}
