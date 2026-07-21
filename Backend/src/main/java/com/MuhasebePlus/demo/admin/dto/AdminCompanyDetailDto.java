package com.MuhasebePlus.demo.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCompanyDetailDto(
        AdminCompanyDto company,
        List<AdminUserDto> users,
        long customerCount,
        long invoiceCount,
        AiQuotaDto aiQuota
) {

    public record AiQuotaDto(
            int monthlyTokenBudget,
            int tokensUsedThisMonth,
            LocalDateTime resetAt
    ) {
    }
}
