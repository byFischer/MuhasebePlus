package com.MuhasebePlus.demo.accounting.dto.response;

import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalEntryResponseDto(
        Long entryId,
        String entryNumber,
        LocalDate entryDate,
        String description,
        JournalSourceType sourceType,
        Long sourceId,
        boolean isReversed,
        Long reversedEntryId,
        List<JournalEntryLineResponseDto> lines,
        LocalDateTime createdAt
) {}
