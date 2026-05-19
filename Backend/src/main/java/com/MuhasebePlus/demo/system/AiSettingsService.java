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
            // No row in app_settings yet — expected on first run before UI setup.
        } catch (Exception ignored) {
            // Table may not exist yet (startup before schema migration) — fall back to property default.
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
