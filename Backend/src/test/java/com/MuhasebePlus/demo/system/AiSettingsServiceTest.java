package com.MuhasebePlus.demo.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiSettingsService için kapsamlı birim testleri. JdbcTemplate mock'lanır;
 * getApiKey'in DB değeri / boş satır / tablo yok / property fallback dalları,
 * setApiKey upsert'i ve isAiEnabled mantığı doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class AiSettingsServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AiSettingsService service;

    private static final String SELECT_SQL =
            "SELECT value FROM app_settings WHERE key = 'ai_api_key'";

    @BeforeEach
    void setUp() {
        // @Value alanı birim testte enjekte edilmediği için elle ayarlanır
        ReflectionTestUtils.setField(service, "defaultApiKey", "PROP_DEFAULT_KEY");
    }

    // ── getApiKey ───────────────────────────────────────────────────────────────

    @Test
    void getApiKey_returnsDbValueWhenPresent() {
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn("db-key-123");

        assertThat(service.getApiKey()).isEqualTo("db-key-123");
    }

    @Test
    void getApiKey_nullDbValue_fallsBackToProperty() {
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn(null);

        assertThat(service.getApiKey()).isEqualTo("PROP_DEFAULT_KEY");
    }

    @Test
    void getApiKey_blankDbValue_fallsBackToProperty() {
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn("   ");

        assertThat(service.getApiKey()).isEqualTo("PROP_DEFAULT_KEY");
    }

    @Test
    void getApiKey_emptyResult_fallsBackToProperty() {
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(service.getApiKey()).isEqualTo("PROP_DEFAULT_KEY");
    }

    @Test
    void getApiKey_genericException_fallsBackToProperty() {
        // Tablo henüz yoksa (migration öncesi) genel Exception dalı çalışır
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class))
                .thenThrow(new RuntimeException("relation \"app_settings\" does not exist"));

        assertThat(service.getApiKey()).isEqualTo("PROP_DEFAULT_KEY");
    }

    @Test
    void getApiKey_nullDefaultProperty_returnsEmptyString() {
        ReflectionTestUtils.setField(service, "defaultApiKey", null);
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn(null);

        assertThat(service.getApiKey()).isEmpty();
    }

    // ── setApiKey ───────────────────────────────────────────────────────────────

    @Test
    void setApiKey_executesUpsertWithGivenKey() {
        service.setApiKey("new-secret");

        verify(jdbcTemplate).update(anyString(), eq("new-secret"));
    }

    // ── isAiEnabled ───────────────────────────────────────────────────────────

    @Test
    void isAiEnabled_trueWhenKeyPresent() {
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn("real-key");

        assertThat(service.isAiEnabled()).isTrue();
    }

    @Test
    void isAiEnabled_falseWhenKeyBlank() {
        // DB null + property boş → getApiKey "" → AI kapalı
        ReflectionTestUtils.setField(service, "defaultApiKey", "");
        when(jdbcTemplate.queryForObject(SELECT_SQL, String.class)).thenReturn(null);

        assertThat(service.isAiEnabled()).isFalse();
    }
}
