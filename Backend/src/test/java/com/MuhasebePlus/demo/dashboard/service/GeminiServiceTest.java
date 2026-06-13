package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.system.AiSettingsService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiServiceTest {

    @Test
    void generate_whenApiKeyMissing_throwsBeforeHttpCall() {
        AiSettingsService settings = mock(AiSettingsService.class);
        when(settings.getApiKey()).thenReturn(" ");
        GeminiService service = new GeminiService(settings);

        assertThatThrownBy(() -> service.generate("system", "prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API");
    }

    @Test
    void generateStructured_whenApiKeyMissing_throwsBeforeHttpCall() {
        AiSettingsService settings = mock(AiSettingsService.class);
        when(settings.getApiKey()).thenReturn(null);
        GeminiService service = new GeminiService(settings);

        assertThatThrownBy(() -> service.generateStructured(
                "system",
                List.of(new ChatMessage("user", "merhaba")),
                Map.of("type", "OBJECT")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("API");
    }

    @Test
    void estimateTokens_usesApproximateFourCharactersPerToken() {
        GeminiService service = new GeminiService(mock(AiSettingsService.class));

        assertThat(service.estimateTokens("12345678")).isEqualTo(2);
        assertThat(service.estimateTokens("123")).isZero();
    }
}
