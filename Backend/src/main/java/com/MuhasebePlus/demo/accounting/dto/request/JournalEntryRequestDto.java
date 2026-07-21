package com.MuhasebePlus.demo.accounting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record JournalEntryRequestDto(
        @NotNull LocalDate entryDate,
        @Size(max = 500) String description,
        @NotEmpty @Valid List<JournalEntryLineRequestDto> lines
) {}
