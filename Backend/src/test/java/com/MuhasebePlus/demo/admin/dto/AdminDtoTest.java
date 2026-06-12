package com.MuhasebePlus.demo.admin.dto;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDtoTest {

    @Test
    void adminUserDtoFromEntity_mapsCompanyAndRole() {
        Company company = company();
        User user = user(10L, "admin@test.com", UserRole.ADMIN, company);

        AdminUserDto dto = AdminUserDto.fromEntity(user);

        assertThat(dto.userId()).isEqualTo(10L);
        assertThat(dto.role()).isEqualTo("ADMIN");
        assertThat(dto.companyId()).isEqualTo(1L);
        assertThat(dto.companyName()).isEqualTo("Test A.S.");
    }

    @Test
    void adminUserDtoFromEntity_handlesNullRoleAndCompany() {
        User user = user(11L, "orphan@test.com", null, null);

        AdminUserDto dto = AdminUserDto.fromEntity(user);

        assertThat(dto.role()).isNull();
        assertThat(dto.companyId()).isNull();
        assertThat(dto.companyName()).isNull();
    }

    @Test
    void adminCompanyDtoFromEntity_mapsCompanyFields() {
        AdminCompanyDto dto = AdminCompanyDto.fromEntity(company(), 4);

        assertThat(dto.companyId()).isEqualTo(1L);
        assertThat(dto.companyName()).isEqualTo("Test A.S.");
        assertThat(dto.userCount()).isEqualTo(4);
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void adminLogDtoFromEntity_mapsUserAndCompany() {
        SystemLog log = log(company(), user(10L, "admin@test.com", UserRole.ADMIN, company()));

        AdminLogDto dto = AdminLogDto.fromEntity(log);

        assertThat(dto.logLevel()).isEqualTo("INFO");
        assertThat(dto.userEmail()).isEqualTo("admin@test.com");
        assertThat(dto.companyName()).isEqualTo("Test A.S.");
    }

    @Test
    void adminLogDtoFromEntity_handlesNullsAndLazyUserFailure() {
        SystemLog log = mock(SystemLog.class);
        when(log.getLogId()).thenReturn(5L);
        when(log.getLogLevel()).thenReturn(null);
        when(log.getDetails()).thenReturn("Detay");
        when(log.getTimestamp()).thenReturn(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(log.getIpAddress()).thenReturn(null);
        when(log.getUserId()).thenReturn(10L);
        when(log.getUser()).thenThrow(new RuntimeException("lazy"));
        when(log.getCompany()).thenReturn(null);

        AdminLogDto dto = AdminLogDto.fromEntity(log);

        assertThat(dto.logLevel()).isNull();
        assertThat(dto.userEmail()).isNull();
        assertThat(dto.companyId()).isNull();
        assertThat(dto.companyName()).isNull();
    }

    @Test
    void recordsExposeConstructorValues() {
        AdminCompanyDto company = new AdminCompanyDto(1L, "Test", "111", "Kadikoy", "Istanbul",
                "555", "info@test.com", true, 2, LocalDateTime.of(2026, 1, 1, 10, 0));
        AdminUserDto user = new AdminUserDto(10L, "A", "B", "a@test.com", "USER", null,
                LocalDate.of(1990, 1, 1), true, false, 0, null, 1L, "Test");
        AdminCompanyDetailDto detail = new AdminCompanyDetailDto(company, List.of(user), 3, 4,
                new AdminCompanyDetailDto.AiQuotaDto(1000, 200, LocalDateTime.of(2026, 7, 1, 0, 0)));
        AdminStatsDto stats = new AdminStatsDto(1, 1, 0, 0, 1, 1, 1, 1, true,
                List.of(new AdminStatsDto.AiConsumerDto(1L, "Test", 200, 1000)), List.of());

        assertThat(detail.company()).isEqualTo(company);
        assertThat(detail.users()).containsExactly(user);
        assertThat(detail.aiQuota().monthlyTokenBudget()).isEqualTo(1000);
        assertThat(stats.aiTopConsumers()).singleElement()
                .extracting(AdminStatsDto.AiConsumerDto::companyName).isEqualTo("Test");
        assertThat(new UpdateUserRoleRequestDto("ADMIN").role()).isEqualTo("ADMIN");
        assertThat(new AdminResetPasswordRequestDto("secret123").newPassword()).isEqualTo("secret123");
    }

    private Company company() {
        return Company.builder()
                .companyId(1L)
                .companyName("Test A.S.")
                .taxNumber("111")
                .taxOffice("Kadikoy")
                .city("Istanbul")
                .phone("555")
                .email("info@test.com")
                .isActive(true)
                .build();
    }

    private User user(Long id, String email, UserRole role, Company company) {
        User user = new User();
        user.setUserId(id);
        user.setCompany(company);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        user.setPhoneNumber("555");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    private SystemLog log(Company company, User user) {
        SystemLog log = new SystemLog();
        log.setLogId(1L);
        log.setLogLevel(LogLevel.INFO);
        log.setDetails("Detay");
        log.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));
        log.setIpAddress("127.0.0.1");
        log.setCompany(company);
        log.setUserId(user.getUserId());
        log.setUser(user);
        return log;
    }
}
