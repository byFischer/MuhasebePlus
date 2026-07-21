package com.MuhasebePlus.demo.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerNoteRequestDto(
    
    @NotBlank(message = "Content is required")
    @Size(max = 5000)
    String content
) 

{
}
