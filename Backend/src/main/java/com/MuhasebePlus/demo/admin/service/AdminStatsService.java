package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminLogDto;
import com.MuhasebePlus.demo.admin.dto.AdminStatsDto;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import com.MuhasebePlus.demo.log.repository.SystemLogRepository;
import com.MuhasebePlus.demo.system.AiSettingsService;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin paneli genel bakış ekranı için platform geneli özet sayılar.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int RECENT_LOG_COUNT = 20;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AiQuotaRepository aiQuotaRepository;
    private final SystemLogRepository systemLogRepository;
    private final AiSettingsService aiSettingsService;

    public AdminStatsDto getStats() {
        List<AdminStatsDto.AiConsumerDto> topConsumers = aiQuotaRepository
                .findTop10ByOrderByTokensUsedThisMonthDesc().stream()
                .filter(q -> q.getTokensUsedThisMonth() > 0)
                .map(q -> new AdminStatsDto.AiConsumerDto(
                        q.getCompanyId(),
                        q.getCompany() != null ? q.getCompany().getCompanyName() : null,
                        q.getTokensUsedThisMonth(),
                        q.getMonthlyTokenBudget()))
                .toList();

        List<AdminLogDto> recentLogs = systemLogRepository
                .findAll(PageRequest.of(0, RECENT_LOG_COUNT, Sort.by(Sort.Direction.DESC, "timestamp")))
                .map(AdminLogDto::fromEntity)
                .getContent();

        return new AdminStatsDto(
                userRepository.count(),
                userRepository.countByIsActiveTrue(),
                userRepository.countByIsLockedTrue(),
                userRepository.countByIsActiveFalse(),
                userRepository.countByRole(UserRole.ADMIN),
                userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30)),
                companyRepository.count(),
                companyRepository.countByIsActiveTrue(),
                aiSettingsService.isAiEnabled(),
                topConsumers,
                recentLogs
        );
    }
}
