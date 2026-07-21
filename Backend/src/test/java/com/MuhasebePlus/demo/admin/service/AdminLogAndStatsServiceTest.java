package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminLogDto;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.log.repository.SystemLogRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.system.AiSettingsService;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLogAndStatsServiceTest {

    @Mock private SystemLogRepository systemLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private AiQuotaRepository aiQuotaRepository;
    @Mock private AiSettingsService aiSettingsService;
    @Mock private SystemLogService systemLogService;

    @InjectMocks private AdminLogService adminLogService;
    @InjectMocks private AdminStatsService adminStatsService;
    @InjectMocks private AdminAuditService adminAuditService;

    @Test
    @SuppressWarnings("unchecked")
    void getLogs_mapsRepositoryPage() {
        PageRequest pageable = PageRequest.of(0, 50);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        SystemLog log = log(1L, LogLevel.INFO, "Detay", company(), user());
        when(systemLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(inv -> {
                    exerciseSpec(inv.getArgument(0));
                    return new PageImpl<>(List.of(log), pageable, 1);
                });

        var result = adminLogService.getLogs(1L, LogLevel.INFO, 10L, true, start, end, pageable);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.logId()).isEqualTo(1L);
            assertThat(dto.userEmail()).isEqualTo("admin@test.com");
            assertThat(dto.companyName()).isEqualTo("Test A.S.");
        });
    }

    @Test
    void exportLogsAsCsv_requiresDateRange() {
        assertThatThrownBy(() -> adminLogService.exportLogsAsCsv(null, null, null, false, null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zorunludur");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportLogsAsCsv_escapesCommasQuotesAndNewLines() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        SystemLog log = log(1L, LogLevel.WARNING, "Virgul, tirnak \" ve\nsatir", company(), user());
        when(systemLogRepository.findAll(any(Specification.class))).thenAnswer(inv -> {
            exerciseSpec(inv.getArgument(0));
            return List.of(log);
        });

        String csv = adminLogService.exportLogsAsCsv(1L, LogLevel.WARNING, 10L, true, start, end);

        assertThat(csv).startsWith("LogID,Level,Timestamp");
        assertThat(csv).contains("\"Virgul, tirnak \"\" ve\nsatir\"");
        assertThat(csv).contains("admin@test.com");
    }

    @Test
    void getStats_aggregatesCountsTopConsumersAndRecentLogs() {
        Company company = company();
        AiQuota usedQuota = new AiQuota(1L, company, 1000, 300,
                LocalDateTime.of(2026, 7, 1, 0, 0), null, null);
        AiQuota unusedQuota = new AiQuota(2L, company, 1000, 0,
                LocalDateTime.of(2026, 7, 1, 0, 0), null, null);
        SystemLog recentLog = log(2L, LogLevel.INFO, "Son log", company, user());
        when(aiQuotaRepository.findTop10ByOrderByTokensUsedThisMonthDesc())
                .thenReturn(List.of(usedQuota, unusedQuota));
        when(systemLogRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(recentLog)));
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActiveTrue()).thenReturn(8L);
        when(userRepository.countByIsLockedTrue()).thenReturn(1L);
        when(userRepository.countByIsActiveFalse()).thenReturn(2L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(3L);
        when(companyRepository.count()).thenReturn(4L);
        when(companyRepository.countByIsActiveTrue()).thenReturn(3L);
        when(aiSettingsService.isAiEnabled()).thenReturn(true);

        var stats = adminStatsService.getStats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.activeUsers()).isEqualTo(8L);
        assertThat(stats.aiEnabled()).isTrue();
        assertThat(stats.aiTopConsumers()).singleElement().satisfies(consumer -> {
            assertThat(consumer.companyId()).isEqualTo(1L);
            assertThat(consumer.tokensUsedThisMonth()).isEqualTo(300);
        });
        assertThat(stats.recentLogs()).singleElement().extracting(AdminLogDto::details).isEqualTo("Son log");
    }

    @Test
    void logAction_prefixesAdminAuditMarker() {
        adminAuditService.logAction("Kullanici silindi");

        verify(systemLogService).log(LogLevel.INFO, "[ADMIN] Kullanici silindi");
    }

    private Company company() {
        return Company.builder()
                .companyId(1L)
                .companyName("Test A.S.")
                .build();
    }

    private User user() {
        User user = new User();
        user.setUserId(10L);
        user.setCompany(company());
        user.setEmail("admin@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    private SystemLog log(Long id, LogLevel level, String details, Company company, User user) {
        SystemLog log = new SystemLog();
        log.setLogId(id);
        log.setCompany(company);
        log.setUserId(user != null ? user.getUserId() : null);
        log.setUser(user);
        log.setLogLevel(level);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.of(2026, 1, 2, 10, 0));
        log.setIpAddress("127.0.0.1");
        return log;
    }

    @SuppressWarnings("unchecked")
    private void exerciseSpec(Specification<SystemLog> spec) {
        spec.toPredicate(
                (Root<SystemLog>) mock(Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
    }
}
