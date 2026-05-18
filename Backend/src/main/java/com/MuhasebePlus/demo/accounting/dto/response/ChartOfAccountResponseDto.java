package com.MuhasebePlus.demo.accounting.dto.response;

import com.MuhasebePlus.demo.accounting.entity.AccountType;

import java.time.LocalDateTime;

public record ChartOfAccountResponseDto(
        Long accountId,
        String accountCode,
        String accountName,
        AccountType accountType,
        Long parentId,
        String parentCode,
        boolean isLeaf,
        boolean isSystem,
        String description,
        LocalDateTime createdAt
) {}
