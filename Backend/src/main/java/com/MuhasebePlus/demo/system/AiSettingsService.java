package com.MuhasebePlus.demo.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiSettingsService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.ai.gemini-api-key:}")
    private String defaultApiKey;

    public AiSettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getApiKey() {
        try {
            String dbKey = jdbcTemplate.queryForObject(
                    "SELECT value FROM app_settings WHERE key = 'ai_api_key'",
                    String.class
            );
            if (dbKey != null && !dbKey.isBlank()) {
                return dbKey;
            }
        } catch (EmptyResultDataAccessException ignored) {
        } catch (Exception ignored) {
            // Tablo henuz yoksa (startup oncesi) default'a don
        }
        return defaultApiKey != null ? defaultApiKey : "";
    }

    public void setApiKey(String apiKey) {
        jdbcTemplate.update(
                "INSERT INTO app_settings (key, value, updated_at) VALUES ('ai_api_key', ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = CURRENT_TIMESTAMP",
                apiKey
        );
    }

    public boolean isAiEnabled() {
        String key = getApiKey();
        return key != null && !key.isBlank();
    }
}
