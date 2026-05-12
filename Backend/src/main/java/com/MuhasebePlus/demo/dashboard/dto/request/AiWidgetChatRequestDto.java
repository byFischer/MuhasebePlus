package com.MuhasebePlus.demo.dashboard.dto.request;

import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AiWidgetChatRequestDto(
        @NotEmpty(message = "Mesaj listesi boş olamaz") List<ChatMessage> messages
) {}
