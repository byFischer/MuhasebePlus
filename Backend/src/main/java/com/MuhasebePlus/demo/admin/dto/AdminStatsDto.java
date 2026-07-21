package com.MuhasebePlus.demo.admin.dto;

import java.util.List;

public record AdminStatsDto(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long passiveUsers,
        long adminCount,
        long newUsersLast30Days,
        long totalCompanies,
        long activeCompanies,
        boolean aiEnabled,
        List<AiConsumerDto> aiTopConsumers,
        List<AdminLogDto> recentLogs
) {

    public record AiConsumerDto(
            Long companyId,
            String companyName,
            int tokensUsedThisMonth,
            int monthlyTokenBudget
    ) {
    }
}
