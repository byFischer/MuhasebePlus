package com.MuhasebePlus.demo.dashboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DashboardLayoutRequestDto(
        @NotBlank @Size(max = 100) String name,
        boolean isDefault,
        String theme,
        String accentColor,
        String layoutPreset
) {}
