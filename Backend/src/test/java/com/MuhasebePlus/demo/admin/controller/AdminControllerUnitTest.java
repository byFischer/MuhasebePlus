package com.MuhasebePlus.demo.admin.controller;

import com.MuhasebePlus.demo.admin.dto.AdminCompanyDetailDto;
import com.MuhasebePlus.demo.admin.dto.AdminCompanyDto;
import com.MuhasebePlus.demo.admin.dto.AdminLogDto;
import com.MuhasebePlus.demo.admin.dto.AdminResetPasswordRequestDto;
import com.MuhasebePlus.demo.admin.dto.AdminStatsDto;
import com.MuhasebePlus.demo.admin.dto.AdminUserDto;
import com.MuhasebePlus.demo.admin.dto.UpdateUserRoleRequestDto;
import com.MuhasebePlus.demo.admin.service.AdminCompanyService;
import com.MuhasebePlus.demo.admin.service.AdminLogService;
import com.MuhasebePlus.demo.admin.service.AdminStatsService;
import com.MuhasebePlus.demo.admin.service.AdminUserService;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerUnitTest {

    @Mock private AdminUserService adminUserService;
    @Mock private AdminCompanyService adminCompanyService;
    @Mock private AdminStatsService adminStatsService;
    @Mock private AdminLogService adminLogService;

    @InjectMocks private AdminUserController adminUserController;
    @InjectMocks private AdminCompanyController adminCompanyController;
    @InjectMocks private AdminStatsController adminStatsController;
    @InjectMocks private AdminLogController adminLogController;

    @Test
    void userEndpoints_delegateAndReturnExpectedStatuses() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminUserDto user = userDto(10L, "admin@test.com", "ADMIN");
        Page<AdminUserDto> page = new PageImpl<>(List.of(user), pageable, 1);
        UpdateUserRoleRequestDto roleRequest = new UpdateUserRoleRequestDto("USER");
        AdminResetPasswordRequestDto passwordRequest = new AdminResetPasswordRequestDto("newSecret1");
        when(adminUserService.getUsers("adm", "ADMIN", "ACTIVE", pageable)).thenReturn(page);
        when(adminUserService.getUser(10L)).thenReturn(user);
        when(adminUserService.updateRole(10L, "USER")).thenReturn(user);

        assertThat(adminUserController.getUsers("adm", "ADMIN", "ACTIVE", pageable).getBody()).isSameAs(page);
        assertThat(adminUserController.getUser(10L).getBody()).isEqualTo(user);
        assertThat(adminUserController.updateRole(10L, roleRequest).getBody()).isEqualTo(user);
        assertThat(adminUserController.lockUser(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminUserController.unlockUser(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminUserController.activateUser(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminUserController.deactivateUser(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminUserController.resetPassword(10L, passwordRequest).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminUserController.deleteUser(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(adminUserService).lockUser(10L);
        verify(adminUserService).unlockUser(10L);
        verify(adminUserService).activateUser(10L);
        verify(adminUserService).deactivateUser(10L);
        verify(adminUserService).resetPassword(10L, "newSecret1");
        verify(adminUserService).deleteUser(10L);
    }

    @Test
    void companyEndpoints_delegateAndReturnExpectedStatuses() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminCompanyDto company = companyDto(1L, true, 2);
        AdminCompanyDetailDto detail = new AdminCompanyDetailDto(company, List.of(userDto(10L, "u@test.com", "USER")),
                5, 7, new AdminCompanyDetailDto.AiQuotaDto(1000, 250, LocalDateTime.of(2026, 7, 1, 0, 0)));
        Page<AdminCompanyDto> page = new PageImpl<>(List.of(company), pageable, 1);
        when(adminCompanyService.getCompanies("test", true, pageable)).thenReturn(page);
        when(adminCompanyService.getCompanyDetail(1L)).thenReturn(detail);

        assertThat(adminCompanyController.getCompanies("test", true, pageable).getBody()).isSameAs(page);
        assertThat(adminCompanyController.getCompanyDetail(1L).getBody()).isEqualTo(detail);
        assertThat(adminCompanyController.activateCompany(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(adminCompanyController.deactivateCompany(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(adminCompanyService).setActive(1L, true);
        verify(adminCompanyService).setActive(1L, false);
    }

    @Test
    void statsEndpoint_returnsServiceStats() {
        AdminStatsDto stats = new AdminStatsDto(10, 8, 1, 1, 2, 3, 4, 3,
                true, List.of(new AdminStatsDto.AiConsumerDto(1L, "Test", 200, 1000)), List.of());
        when(adminStatsService.getStats()).thenReturn(stats);

        ResponseEntity<AdminStatsDto> response = adminStatsController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(stats);
    }

    @Test
    void logEndpoints_delegateAndExportCsvWithHeaders() {
        PageRequest pageable = PageRequest.of(0, 50);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        AdminLogDto log = new AdminLogDto(1L, "INFO", "Detay", LocalDateTime.of(2026, 1, 2, 10, 0),
                "127.0.0.1", 10L, "admin@test.com", 1L, "Test A.S.");
        Page<AdminLogDto> page = new PageImpl<>(List.of(log), pageable, 1);
        when(adminLogService.getLogs(1L, LogLevel.INFO, 10L, true, start, end, pageable)).thenReturn(page);
        when(adminLogService.exportLogsAsCsv(1L, LogLevel.INFO, 10L, true, start, end))
                .thenReturn("LogID,Details\n1,Detay\n");

        assertThat(adminLogController.getLogs(1L, LogLevel.INFO, 10L, true, start, end, pageable).getBody()).isSameAs(page);

        ResponseEntity<byte[]> response = adminLogController.exportLogs(1L, LogLevel.INFO, 10L, true, start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("admin_logs.csv");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(response.getBody().length);
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8)).contains("Detay");
    }

    private AdminUserDto userDto(Long id, String email, String role) {
        return new AdminUserDto(id, "Test", "User", email, role, "555", LocalDate.of(1990, 1, 1),
                true, false, 0, LocalDateTime.of(2026, 1, 1, 10, 0), 1L, "Test A.S.");
    }

    private AdminCompanyDto companyDto(Long id, boolean active, long userCount) {
        return new AdminCompanyDto(id, "Test A.S.", "111", "Kadikoy", "Istanbul", "555", "info@test.com",
                active, userCount, LocalDateTime.of(2026, 1, 1, 10, 0));
    }
}
