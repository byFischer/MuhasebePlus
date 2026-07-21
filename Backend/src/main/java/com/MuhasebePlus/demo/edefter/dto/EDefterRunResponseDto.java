package com.MuhasebePlus.demo.edefter.dto;

import com.MuhasebePlus.demo.edefter.entity.EDefterStatus;

import java.time.LocalDateTime;

public record EDefterRunResponseDto(
        Long runId,
        Integer year,
        Integer month,
        EDefterStatus status,
        Integer journalEntryCount,
        Integer journalLineCount,
        LocalDateTime generatedAt
) {}
