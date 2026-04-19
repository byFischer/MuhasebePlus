package com.MuhasebePlus.demo.customer.dto.response;

import java.time.LocalDateTime;

public record CustomerNoteResponseDto(
    Long noteId,
    Long customerId,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) 
{    
}
