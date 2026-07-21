package com.MuhasebePlus.demo.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiSettingsService {

    @Value("${app.ai.gemini-api-key:}")
    private String defaultApiKey;

    public String getApiKey() {
        return defaultApiKey != null ? defaultApiKey : "";
    }

    public boolean isAiEnabled() {
        String key = getApiKey();
        return key != null && !key.isBlank();
    }
}
