package com.MuhasebePlus.demo.accounting.dto.request;

import com.MuhasebePlus.demo.accounting.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChartOfAccountRequestDto(
        @NotBlank @Size(max = 20) String accountCode,
        @NotBlank @Size(max = 255) String accountName,
        @NotNull AccountType accountType,
        Long parentId,
        Boolean isLeaf,
        @Size(max = 500) String description
) {}
